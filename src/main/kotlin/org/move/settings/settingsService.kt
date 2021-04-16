package org.move.settings

import com.intellij.configurationStore.serializeObjectInto
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

interface MoveSettingsListener {
    fun moveSettingsChanged(e: MoveProjectSettingsService.MoveSettingsChangedEvent)
}


interface MoveProjectSettingsService {

    data class State(
        var doveExecutablePath: String = "",
    )

    var settingsState: State

//    val doveExecutable: Path?

    /**
     * Allows to modify settings.
     * After setting change,
     */
//    fun modify(action: (State) -> Unit)

//    @TestOnly
//    fun modifyTemporary(parentDisposable: Disposable, action: (State) -> Unit)

    /**
     * Returns current state of the service.
     * Note, result is a copy of service state, so you need to set modified state back to apply changes
     */
//    var settingsState: State

    companion object {
        val MOVE_SETTINGS_TOPIC = Topic(
            "move settings changes",
            MoveSettingsListener::class.java)
    }

    data class MoveSettingsChangedEvent(val oldState: State, val newState: State)
}

private const val serviceName: String = "MoveProjectSettings"

@com.intellij.openapi.components.State(name = serviceName, storages = [
    Storage(StoragePathMacros.WORKSPACE_FILE),
    Storage("misc.xml", deprecated = true)
])
class MoveProjectSettingsServiceImpl(private val project: Project) : MoveProjectSettingsService,
                                                                     PersistentStateComponent<Element> {
    @Volatile
    private var _state = MoveProjectSettingsService.State()

    override var settingsState: MoveProjectSettingsService.State
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
        oldState: MoveProjectSettingsService.State,
        newState: MoveProjectSettingsService.State,
    ) {
        val event = MoveProjectSettingsService.MoveSettingsChangedEvent(oldState, newState)
        project.messageBus.syncPublisher(MoveProjectSettingsService.MOVE_SETTINGS_TOPIC)
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
}

val Project.moveSettings: MoveProjectSettingsService
    get() = ServiceManager.getService(this, MoveProjectSettingsService::class.java)
        ?: error("Failed to get MoveProjectSettingsService for $this")

val Project.dovePath: String get() {
    return this.moveSettings.settingsState.doveExecutablePath
}