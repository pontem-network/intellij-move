package org.move.settings

import com.intellij.configurationStore.serializeObjectInto
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KProperty1

data class MvSettingsChangedEvent(
    val oldState: MvProjectSettingsService.State,
    val newState: MvProjectSettingsService.State,
) {
    /** Use it like `event.isChanged(State::foo)` to check whether `foo` property is changed or not */
    fun isChanged(prop: KProperty1<MvProjectSettingsService.State, *>): Boolean =
        prop.get(oldState) != prop.get(newState)
}

interface MvSettingsListener {
    fun moveSettingsChanged(e: MvSettingsChangedEvent)
}

private const val serviceName: String = "MoveProjectSettings"

@Service
@com.intellij.openapi.components.State(
    name = serviceName, storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE),
        Storage("misc.xml", deprecated = true)
    ]
)
class MvProjectSettingsService(private val project: Project) : PersistentStateComponent<Element> {

    data class State(
        var isDove: Boolean = true,
        var isAptos: Boolean = false,
        var moveExecutablePath: String = "",
        var collapseSpecs: Boolean = false,
    )

    @Volatile
    private var _state = State()

    var settingsState: State
        get() = _state.copy()
        set(newState) {
            if (_state != newState) {
                val oldState = _state
                _state = newState.copy()
                notifySettingsChanged(oldState, newState)
            }
        }

    fun showMvConfigureSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, MoveConfigurable::class.java)
    }

    private fun notifySettingsChanged(
        oldState: State,
        newState: State,
    ) {
        val event = MvSettingsChangedEvent(oldState, newState)
        project.messageBus.syncPublisher(MOVE_SETTINGS_TOPIC)
            .moveSettingsChanged(event)

        if (event.isChanged(State::collapseSpecs)) {
            PsiManager.getInstance(project).dropPsiCaches()
        }
    }

    override fun getState(): Element {
        val element = Element(serviceName)
        serializeObjectInto(_state, element)
        return element
    }

    override fun loadState(element: Element) {
        val rawState = element.clone()
        XmlSerializer.deserializeInto(_state, rawState)
    }

    /**
     * Allows to modify settings.
     * After setting change,
     */
    @TestOnly
    fun modifyTemporary(parentDisposable: Disposable, action: (State) -> Unit) {
        val oldState = settingsState
        settingsState = oldState.also(action)
        Disposer.register(parentDisposable) {
            _state = oldState
        }
    }

    /**
     * Returns current state of the service.
     * Note, result is a copy of service state, so you need to set modified state back to apply changes
     */
    companion object {
        val MOVE_SETTINGS_TOPIC = Topic(
            "move settings changes",
            MvSettingsListener::class.java
        )
    }
}

val Project.moveSettings: MvProjectSettingsService
    get() = this.getService(MvProjectSettingsService::class.java)

enum class ProjectKind {
    DOVE, APTOS;
}

val Project.kind: ProjectKind
    get() {
        return if (this.moveSettings.settingsState.isAptos) {
            ProjectKind.APTOS
        } else {
            ProjectKind.DOVE
        }
    }

@TestOnly
fun Project.setKind(kind: ProjectKind, disposable: Disposable) {
    when (kind) {
        ProjectKind.DOVE -> {
            this.moveSettings.modifyTemporary(disposable) {
                it.isAptos = false
                it.isDove = true
            }
        }
        ProjectKind.APTOS -> {
            this.moveSettings.modifyTemporary(disposable) {
                it.isAptos = true
                it.isDove = false
            }
        }
    }
}

val Project.moveExecutablePathValue: String
    get() {
        return this.moveSettings.settingsState.moveExecutablePath
    }

val Project.collapseSpecs: Boolean
    get() {
        return this.moveSettings.settingsState.collapseSpecs
    }

val Project.moveBinaryPath: Path?
    get() {
        val value = this.moveExecutablePathValue
        if (value.isBlank()) return null

        val path = Paths.get(value)
        if (!path.toFile().exists()) return null

        return path
    }
