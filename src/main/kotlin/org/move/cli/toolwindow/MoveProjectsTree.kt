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
//        addMouseListener(object : MouseAdapter() {
//            override fun mouseClicked(e: MouseEvent) {
//                if (e.clickCount < 2 || !SwingUtilities.isLeftMouseButton(e)) return
////                val tree = e.source as? MoveProjectsTree ?: return
////                val node = tree.selectionModel.selectionPath
////                    ?.lastPathComponent as? DefaultMutableTreeNode ?: return
////                val target = (node.userObject as? MoveSim.Target)?.target ?: return
////                val command = target.launchCommand()
////                if (command == null) {
////                    LOG.warn("Can't create launch command for `${target.name}` target")
////                    return
////                }
////                val cargoProject = selectedProject ?: return
////                CargoCommandLine.forTarget(target, command).run(cargoProject)
//            }
//        })
    }
}
