package org.move.cli.transactions

import org.move.cli.toolwindow.MoveProjectsTree
import org.move.cli.toolwindow.MoveProjectsTreeStructure
import org.move.lang.moveProject
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode

class MoveEntrypointMouseAdapter : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount < 2 || !SwingUtilities.isLeftMouseButton(e)) return

        val tree = e.source as? MoveProjectsTree ?: return
        val node = tree.selectionModel.selectionPath
            ?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val entryFunction =
            (node.userObject as? MoveProjectsTreeStructure.MoveSimpleNode.Entrypoint)?.function
                ?: return

        val moveProject = entryFunction.moveProject ?: return

        val paramsDialog = RunTransactionDialog.showAndWaitTillOk(entryFunction, moveProject) ?: return
        paramsDialog
            .toAptosCommandLineFromContext()
            ?.createRunConfigurationAndRun(moveProject)
    }
}
