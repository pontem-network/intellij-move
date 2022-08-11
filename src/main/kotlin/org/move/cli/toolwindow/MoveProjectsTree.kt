package org.move.cli.toolwindow

import com.intellij.ui.treeStructure.SimpleTree
import org.move.cli.MoveProject
import org.move.cli.scripts.RunScriptDialog
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
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
//        addMouseListener(object : MouseAdapter() {
//            override fun mouseClicked(e: MouseEvent) {
//                if (e.clickCount < 2 || !SwingUtilities.isLeftMouseButton(e)) return
//
//                val tree = e.source as? MoveProjectsTree ?: return
//                val node = tree.selectionModel.selectionPath
//                    ?.lastPathComponent as? DefaultMutableTreeNode ?: return
//                val scriptFunction =
//                    (node.userObject as? MoveProjectsTreeStructure.MoveSimpleNode.Entrypoint)?.function
//                        ?: return
//                val runScriptDialog = RunScriptDialog(scriptFunction)
////                runScriptDialog.show()
//            }
//        })
    }
}
