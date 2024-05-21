package org.move.bytecode

import com.intellij.openapi.actionSystem.AnActionEvent
import org.move.cli.runConfigurations.aptos.RunAptosCommandActionBase

class FetchAptosPackageAction: RunAptosCommandActionBase("Fetch on-chain package") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val parametersDialog = FetchAptosPackageDialog(project)
        parametersDialog.show()
    }

}