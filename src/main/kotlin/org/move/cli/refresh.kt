package org.move.cli

import com.intellij.openapi.project.Project
import org.move.settings.MoveProjectSettingsService
import org.move.settings.MoveSettingsListener

class RefreshProjectListener(val project: Project) : MoveSettingsListener {
    override fun moveSettingsChanged(e: MoveProjectSettingsService.MoveSettingsChangedEvent) {
        refreshProject(project)
    }
}

fun refreshProject(project: Project) {
    println("refreshProject called")
}