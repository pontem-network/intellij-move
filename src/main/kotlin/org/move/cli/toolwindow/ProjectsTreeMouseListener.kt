package org.move.cli.toolwindow

import com.intellij.util.PsiNavigateUtil
import org.move.cli.toolwindow.MoveProjectsTreeStructure.MoveSimpleNode
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
            is MoveSimpleNode.Entrypoint -> {
                PsiNavigateUtil.navigate(userNode.function)
            }
            is MoveSimpleNode.View -> {
                PsiNavigateUtil.navigate(userNode.function)
            }
            is MoveSimpleNode.Module -> {
                PsiNavigateUtil.navigate(userNode.module)
            }
        }
    }
}
