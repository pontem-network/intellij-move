package org.move.project.settings.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.TestOnly
import org.move.project.settings.MoveProjectSettingsService
import org.move.project.settings.MoveProjectSettingsService.State
import java.nio.file.Path
import java.nio.file.Paths

private const val serviceName: String = "MoveProjectSettings"

@com.intellij.openapi.components.State(name = serviceName, storages = [
    Storage(StoragePathMacros.WORKSPACE_FILE),
])
class MoveProjectSettingsServiceImpl : MoveProjectSettingsService {
    @Volatile
    private var _state = State()

    override var settingsState: State
        get() = _state.copy()
        set(newState) {
            if (_state != newState) {
//                val oldState = _state
                _state = newState.copy()
//                notifySettingsChanged(oldState, newState)
            }
        }

    override val doveExecutable: Path?
        get() = _state.doveExecutable?.let { Paths.get(it) }

    override fun modify(action: (State) -> Unit) {
        settingsState = settingsState.also(action)
    }

    @TestOnly
    override fun modifyTemporary(parentDisposable: Disposable, action: (State) -> Unit) {
        val oldState = settingsState
        settingsState = oldState.also(action)
        Disposer.register(parentDisposable) {
            _state = oldState
        }
    }

//    private fun notifySettingsChanged(oldState: State, newState: State) {
//        val event = RustSettingsChangedEvent(oldState, newState)
//        project.messageBus.syncPublisher(RUST_SETTINGS_TOPIC).rustSettingsChanged(event)
//
//        if (event.isChanged(State::doctestInjectionEnabled)) {
//            // flush injection cache
//            (PsiManager.getInstance(project).modificationTracker as PsiModificationTrackerImpl).incCounter()
//        }
//        if (event.isChanged(State::newResolveEnabled)) {
//            project.defMapService.onNewResolveEnabledChanged(newState.newResolveEnabled)
//        }
//        if (event.affectsHighlighting) {
//            DaemonCodeAnalyzer.getInstance(project).restart()
//        }
//    }
}