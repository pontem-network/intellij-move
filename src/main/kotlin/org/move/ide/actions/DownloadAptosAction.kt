package org.move.ide.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.move.ide.actions.download.PathSelector
import org.move.ide.actions.download.ui.DownloadAptosDialog

class DownloadAptosAction: DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val currentProject = event.project ?: return
        DownloadAptosDialog(currentProject, "2.0.2").show()
    }
}
