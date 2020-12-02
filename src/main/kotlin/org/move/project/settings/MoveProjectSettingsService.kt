package org.move.project.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path

interface MoveProjectSettingsService {
    data class State(
        var doveExecutable: String? = null,
    )

    val doveExecutable: Path?

    /**
     * Allows to modify settings.
     * After setting change,
     */
    fun modify(action: (State) -> Unit)

    @TestOnly
    fun modifyTemporary(parentDisposable: Disposable, action: (State) -> Unit)

    /**
     * Returns current state of the service.
     * Note, result is a copy of service state, so you need to set modified state back to apply changes
     */
    var settingsState: State
}

val Project.moveSettings: MoveProjectSettingsService
    get() = ServiceManager.getService(this, MoveProjectSettingsService::class.java)
        ?: error("Failed to get MoveProjectSettingsService for $this")