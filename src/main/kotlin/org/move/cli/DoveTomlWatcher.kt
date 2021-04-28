package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent


class DoveTomlWatcher(val project: Project) : BulkFileListener {

    override fun after(events: List<VFileEvent>) {
        if (events.any { it.pathEndsWith(DoveConstants.MANIFEST_FILE) }) {
            refreshProject(project)
        }
    }

    private fun VFileEvent.pathEndsWith(suffix: String): Boolean = path.endsWith(suffix) ||
            this is VFilePropertyChangeEvent && oldPath.endsWith(suffix)
}