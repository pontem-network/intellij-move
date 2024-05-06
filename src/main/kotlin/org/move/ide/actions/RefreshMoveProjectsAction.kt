package org.move.ide.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.move.cli.moveProjectsService
import org.move.openapiext.saveAllDocuments

class RefreshMoveProjectsAction: DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        saveAllDocuments()
        project.moveProjectsService.scheduleProjectsRefresh("Refresh Action called")
    }
}
