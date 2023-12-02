package org.move.cli.settings

import com.intellij.configurationStore.serializeObjectInto
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.annotations.TestOnly
import org.move.stdext.exists
import org.move.stdext.isExecutableFile
import java.nio.file.Path
import kotlin.reflect.KProperty1

data class MoveSettingsChangedEvent(
    val oldState: MoveProjectSettingsService.State,
    val newState: MoveProjectSettingsService.State,
) {
    /** Use it like `event.isChanged(State::foo)` to check whether `foo` property is changed or not */
    fun isChanged(prop: KProperty1<MoveProjectSettingsService.State, *>): Boolean =
        prop.get(oldState) != prop.get(newState)
}

interface MoveSettingsListener {
    fun moveSettingsChanged(e: MoveSettingsChangedEvent)
}

private const val settingsServiceName: String = "MoveProjectSettingsService_1"

@Service(Service.Level.PROJECT)
@State(
    name = settingsServiceName,
    storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE),
        Storage("misc.xml", deprecated = true)
    ]
)
class MoveProjectSettingsService(private val project: Project): PersistentStateComponent<Element> {

    // default values for settings
    data class State(
        // null not Mac -> Bundled, null and Mac -> Local(""), not null -> Local(value)
        var aptosPath: String? = if (AptosExec.isBundledSupported()) null else "",
        var foldSpecs: Boolean = false,
        var disableTelemetry: Boolean = true,
        var debugMode: Boolean = false,
        var skipFetchLatestGitDeps: Boolean = false
    )

    @Volatile
    private var _state = State()

    var state: State
        get() = _state.copy()
        set(newState) {
            if (_state != newState) {
                val oldState = _state
                _state = newState.copy()
                notifySettingsChanged(oldState, newState)
            }
        }

    private fun notifySettingsChanged(
        oldState: State,
        newState: State,
    ) {
        val event = MoveSettingsChangedEvent(oldState, newState)
        project.messageBus.syncPublisher(MOVE_SETTINGS_TOPIC)
            .moveSettingsChanged(event)

        if (event.isChanged(State::foldSpecs)) {
            PsiManager.getInstance(project).dropPsiCaches()
        }
    }

    override fun getState(): Element {
        val element = Element(settingsServiceName)
        serializeObjectInto(_state, element)
        return element
    }

    override fun loadState(element: Element) {
        val rawState = element.clone()
        XmlSerializer.deserializeInto(_state, rawState)
        // migrate old default value for MacOS aptosPath (bundled is not supported anymore)
//        if (_state.aptosPath == null && !AptosExec.isBundledSupported()) {
//            _state.aptosPath = ""
//        }
    }

    /**
     * Allows to modify settings.
     * After setting change,
     */
    fun modify(action: (State) -> Unit) {
        val oldState = state.copy()
        val newState = state.also(action)
        val event = MoveSettingsChangedEvent(oldState, newState)
        project.messageBus.syncPublisher(MOVE_SETTINGS_TOPIC).moveSettingsChanged(event)
    }

    /**
     * Allows to modify settings.
     * After setting change,
     */
    @TestOnly
    fun modifyTemporary(parentDisposable: Disposable, action: (State) -> Unit) {
        val oldState = state
        state = oldState.also(action)
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
            MoveSettingsListener::class.java
        )
    }
}

val Project.moveSettings: MoveProjectSettingsService get() = service()

val Project.collapseSpecs: Boolean get() = this.moveSettings.state.foldSpecs

val Project.aptosExec: AptosExec get() = AptosExec.fromSettingsFormat(this.moveSettings.state.aptosPath)

val Project.aptosPath: Path? get() = this.aptosExec.pathOrNull()

fun Path?.isValidExecutable(): Boolean {
    return this != null
            && this.toString().isNotBlank()
            && this.exists()
            && this.isExecutableFile()
}

val Project.isDebugModeEnabled: Boolean get() = this.moveSettings.state.debugMode

fun <T> Project.debugErrorOrFallback(message: String, fallback: T): T {
    if (this.isDebugModeEnabled) {
        error(message)
    }
    return fallback
}

fun <T> Project.debugErrorOrFallback(message: String, cause: Throwable?, fallback: () -> T): T {
    if (this.isDebugModeEnabled) {
        throw IllegalStateException(message, cause)
    }
    return fallback()
}

val Project.skipFetchLatestGitDeps: Boolean
    get() =
        this.moveSettings.state.skipFetchLatestGitDeps
