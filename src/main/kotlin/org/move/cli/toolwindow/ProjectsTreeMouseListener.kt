package org.move.cli.toolwindow

import com.intellij.diagnostic.PluginException
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.RunContextAction
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.diagnostic.LogLevel.WARNING
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import org.move.cli.toolwindow.MoveProjectsTreeStructure.MoveSimpleNode
import org.move.ide.notifications.logOrShowBalloon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode

class ProjectsTreeMouseListener: MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
        // trigger listener only with left double-click
        if (!SwingUtilities.isLeftMouseButton(e) || e.clickCount < 2) return

        val projectsTree = e.source as? MoveProjectsTree ?: return
        val treeNode = projectsTree.selectionModel
            .selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val userNode = treeNode.userObject
        when (userNode) {
            is MoveSimpleNode.Entrypoint -> navigateToPsiElement(userNode.function)
            is MoveSimpleNode.View -> navigateToPsiElement(userNode.function)
            is MoveSimpleNode.Module -> navigateToPsiElement(userNode.module)
        }
    }

    private fun executeContextAction(psiElement: PsiElement) {
        val psiLocation =
            try {
                PsiLocation.fromPsiElement(psiElement) ?: return
            } catch (e: PluginException) {
                // TODO: figure out why this exception is raised
                LOG.logOrShowBalloon(e.message, productionLevel = WARNING)
                return
            }
        val dataContext =
            SimpleDataContext.getSimpleContext(Location.DATA_KEY, psiLocation)
        val actionEvent =
            AnActionEvent.createFromDataContext(ActionPlaces.TOOLWINDOW_CONTENT, null, dataContext)

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        RunContextAction(executor).actionPerformed(actionEvent)
    }

    private fun navigateToPsiElement(psiElement: PsiElement) {
        val navigationElement = psiElement.navigationElement as? NavigatablePsiElement ?: return
        if (navigationElement.canNavigate()) {
            navigationElement.navigate(true)
        }
    }

    companion object {
        private val LOG = logger<ProjectsTreeMouseListener>()
    }
}
