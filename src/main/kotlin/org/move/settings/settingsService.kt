package org.move.settings

import com.intellij.configurationStore.serializeObjectInto
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.annotations.TestOnly
import org.move.cli.DoveExecutable
import java.nio.file.Paths

data class MoveSettingsChangedEvent(
    val oldState: MoveProjectSettingsService.State,
    val newState: MoveProjectSettingsService.State,
)

interface MoveSettingsListener {
    fun moveSettingsChanged(e: MoveSettingsChangedEvent)
}

private const val serviceName: String = "MoveProjectSettings"

@Service
@com.intellij.openapi.components.State(name = serviceName, storages = [
    Storage(StoragePathMacros.WORKSPACE_FILE),
    Storage("misc.xml", deprecated = true)
])
class MoveProjectSettingsService(private val project: Project) : PersistentStateComponent<Element> {

    data class State(
        var doveExecutablePath: String = "",
    )

    @Volatile
    private var _state = State()

    var settingsState: State
        get() = _state.copy()
        set(newState) {
            println("set newState $newState")
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
    }

    override fun getState(): Element {
        val element = Element(serviceName)
        serializeObjectInto(_state, element)
        println("getState $_state")
        return element
    }

    override fun loadState(element: Element) {
        val rawState = element.clone()
        XmlSerializer.deserializeInto(_state, rawState)
        println("loadState $_state")
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
//    @TestOnly
//    fun modifyTemporary(parentDisposable: Disposable, action: (State) -> Unit)

    /**
     * Returns current state of the service.
     * Note, result is a copy of service state, so you need to set modified state back to apply changes
     */
    companion object {
        val MOVE_SETTINGS_TOPIC = Topic(
            "move settings changes",
            MoveSettingsListener::class.java)
    }
}

val Project.moveSettings: MoveProjectSettingsService
    get() = ServiceManager.getService(this, MoveProjectSettingsService::class.java)

val Project.dovePath: String
    get() {
        return this.moveSettings.settingsState.doveExecutablePath
    }

fun Project.getDoveExecutable(): DoveExecutable? {
    val value = this.dovePath
    if (value.isBlank()) return null

    val path = Paths.get(value)
    if (!path.toFile().exists()) return null

    return DoveExecutable(path)
}