package org.move.cli.transactions

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.RunContextAction
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import org.move.cli.toolwindow.MoveProjectsTree
import org.move.cli.toolwindow.MoveProjectsTreeStructure
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
        val function =
            (node.userObject as? MoveProjectsTreeStructure.MoveSimpleNode.Entrypoint)?.function
                ?: return

        val dataContext =
            SimpleDataContext.getSimpleContext(Location.DATA_KEY, PsiLocation.fromPsiElement(function))
        val actionEvent =
            AnActionEvent.createFromDataContext(ActionPlaces.TOOLWINDOW_CONTENT, null, dataContext)

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        RunContextAction(executor).actionPerformed(actionEvent)
    }
}
