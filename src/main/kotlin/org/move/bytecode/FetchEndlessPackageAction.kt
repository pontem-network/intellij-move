package org.move.bytecode

import com.intellij.openapi.actionSystem.AnActionEvent
import org.move.cli.runConfigurations.endless.RunEndlessCommandActionBase

class FetchEndlessPackageAction: RunEndlessCommandActionBase("Fetch on-chain package") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val parametersDialog = FetchEndlessPackageDialog(project)
        parametersDialog.show()
    }

}