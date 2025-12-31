package org.move.ide.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.move.cli.settings.PerProjectEndlessConfigurable
import org.move.openapiext.showSettingsDialog

class MoveEditSettingsAction : DumbAwareAction("Endless Settings") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.showSettingsDialog<PerProjectEndlessConfigurable>()
    }
}
