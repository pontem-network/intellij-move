package org.move.cli.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubTreeLoader
import com.intellij.util.FileContentUtilCore
import com.intellij.util.messages.Topic
import org.jetbrains.annotations.TestOnly
import org.move.cli.settings.MvProjectSettingsServiceBase.MvProjectSettingsBase
import org.move.lang.MoveFileType
import org.move.openapiext.saveAllDocuments
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

abstract class MvProjectSettingsServiceBase<T: MvProjectSettingsBase<T>>(
    val project: Project,
    state: T
): SimplePersistentStateComponent<T>(state) {

    abstract class MvProjectSettingsBase<T: MvProjectSettingsBase<T>>: BaseState() {
        abstract fun copy(): T
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.PROPERTY)
    protected annotation class AffectsMoveProjectsMetadata

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.PROPERTY)
    protected annotation class AffectsHighlighting

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.PROPERTY)
    protected annotation class AffectsParseTree

    fun modify(modifySettings: (T) -> Unit) {
        val oldState = this.state.copy()
        // assigns new values to `this.state`
        val newState = this.state.also(modifySettings)

        val event = createSettingsChangedEvent(oldState, newState)
        notifySettingsChanged(event)
    }

    @TestOnly
    fun modifyTemporary(parentDisposable: Disposable, modifySettings: (T) -> Unit) {
        val oldState = state
        loadState(oldState.copy().also(modifySettings))
        Disposer.register(parentDisposable) {
            loadState(oldState)
        }
    }

    companion object {
        val MOVE_SETTINGS_TOPIC: Topic<MoveSettingsListener> = Topic.create(
            "move settings changes",
            MoveSettingsListener::class.java,
//            Topic.BroadcastDirection.TO_PARENT
        )
    }

    interface MoveSettingsListener {
        fun <T: MvProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>)
    }

    protected abstract fun createSettingsChangedEvent(oldEvent: T, newEvent: T): SettingsChangedEventBase<T>

    protected open fun notifySettingsChanged(event: SettingsChangedEventBase<T>) {
        project.messageBus.syncPublisher(MOVE_SETTINGS_TOPIC).settingsChanged(event)

        if (event.affectsHighlighting) {
            DaemonCodeAnalyzer.getInstance(project).restart()
        }

        if (event.affectsParseTree) {
            // refresh all .move files (didn't find any proper way to do it)
            saveAllDocuments()

            val moveFiles = FileTypeIndex.getFiles(MoveFileType, GlobalSearchScope.allScope(project))

            PsiManager.getInstance(project).dropPsiCaches()
            FileContentUtilCore.reparseFiles(moveFiles)
            moveFiles.forEach { StubTreeLoader.getInstance().rebuildStubTree(it) }
        }
    }

    abstract class SettingsChangedEventBase<T: MvProjectSettingsBase<T>>(val oldState: T, val newState: T) {
        private val moveProjectsMetadataAffectingProps: List<KProperty1<T, *>> =
            oldState.javaClass.kotlin.memberProperties.filter { it.findAnnotation<AffectsMoveProjectsMetadata>() != null }

        private val highlightingAffectingProps: List<KProperty1<T, *>> =
            oldState.javaClass.kotlin.memberProperties.filter { it.findAnnotation<AffectsHighlighting>() != null }

        private val parseTreeAffectingProps: List<KProperty1<T, *>> =
            oldState.javaClass.kotlin.memberProperties.filter { it.findAnnotation<AffectsParseTree>() != null }

        val affectsMoveProjectsMetadata: Boolean
            get() = moveProjectsMetadataAffectingProps.any(::isChanged)

        val affectsHighlighting: Boolean
            get() = highlightingAffectingProps.any(::isChanged)

        val affectsParseTree: Boolean
            get() = parseTreeAffectingProps.any(::isChanged)

        /** Use it like `event.isChanged(State::foo)` to check whether `foo` property is changed or not */
        fun isChanged(prop: KProperty1<T, *>): Boolean = prop.get(oldState) != prop.get(newState)
    }
}
