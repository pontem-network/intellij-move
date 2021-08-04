package org.move.cli

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.move.settings.MoveSettingsChangedEvent
import org.move.settings.MoveSettingsListener

class RefreshProjectListener(
    val project: Project
) : MoveSettingsListener {

    override fun moveSettingsChanged(e: MoveSettingsChangedEvent) = refreshProject(project)
}

fun refreshProject(project: Project) {
//    ServiceManager.getService(project, MetadataService::class.java).refresh()
}
