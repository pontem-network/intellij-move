package org.move.cli.toolwindow

import com.intellij.ui.treeStructure.SimpleTree
import org.move.cli.MoveProject
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

class MoveProjectsTree : SimpleTree() {

    val selectedProject: MoveProject?
        get() {
            val path = selectionPath ?: return null
            if (path.pathCount < 2) return null
            val treeNode = path.getPathComponent(1) as? DefaultMutableTreeNode ?: return null
            return (treeNode.userObject as? MoveProjectsTreeStructure.MoveSimpleNode.Project)?.moveProject
        }

    init {
        isRootVisible = false
        showsRootHandles = true
        emptyText.text = "There are no Move projects to display."
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        addMouseListener(MoveEntrypointMouseAdapter())
    }
}
