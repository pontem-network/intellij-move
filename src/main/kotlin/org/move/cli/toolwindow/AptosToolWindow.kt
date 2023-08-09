package org.move.cli.toolwindow

import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.content.ContentFactory
import org.move.cli.MoveProject
import org.move.cli.MoveProjectsService
import org.move.cli.MoveProjectsService.MoveProjectsListener
import org.move.cli.hasMoveProject
import org.move.cli.moveProjects
import javax.swing.JComponent

class AptosToolWindowFactory : ToolWindowFactory, DumbAware {
    private val lock: Any = Any()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
//        guessAndSetupRustProject(project)
        project.moveProjects.refreshAllProjects()

        val toolwindowPanel = AptosToolWindowPanel(project)
        val tab = ContentFactory.getInstance()
            .createContent(toolwindowPanel, "", false)
        toolWindow.contentManager.addContent(tab)
    }

//    override fun isApplicable(project: Project): Boolean {
//        if (MoveToolWindow.isRegistered(project)) return false
//
////        val cargoProjects = project.moveProjects
////        if (!cargoProjects.hasAtLeastOneValidProject
////            && cargoProjects.suggestManifests().none()
////        ) return false
//
////        synchronized(lock) {
////            val res = project.getUserData(CARGO_TOOL_WINDOW_APPLICABLE) ?: true
////            if (res) {
////                project.putUserData(CARGO_TOOL_WINDOW_APPLICABLE, false)
////            }
////            return res
////        }
//    }
}

private class AptosToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, false) {
    private val aptosTab = AptosToolWindow(project)

    init {
        toolbar = aptosTab.toolbar.component
        aptosTab.toolbar.setTargetComponent(this)
        setContent(aptosTab.content)
    }

    override fun getData(dataId: String): Any? =
        when {
            AptosToolWindow.SELECTED_MOVE_PROJECT.`is`(dataId) -> aptosTab.selectedProject
            PlatformDataKeys.TREE_EXPANDER.`is`(dataId) -> aptosTab.treeExpander
            else -> super.getData(dataId)
        }
}

class AptosToolWindow(private val project: Project) {

    val toolbar: ActionToolbar = run {
        val actionManager = ActionManager.getInstance()
        actionManager.createActionToolbar(
            APTOS_TOOLBAR_PLACE,
            actionManager.getAction("Move.Aptos") as DefaultActionGroup,
            true
        )
    }

    private val projectTree = MoveProjectsTree()
    private val projectStructure = MoveProjectsTreeStructure(projectTree, project)

    val treeExpander: TreeExpander = object : DefaultTreeExpander(projectTree) {
        override fun isCollapseAllVisible(): Boolean = project.hasMoveProject
        override fun isExpandAllVisible(): Boolean = project.hasMoveProject
    }

    val selectedProject: MoveProject? get() = projectTree.selectedProject

    val content: JComponent = ScrollPaneFactory.createScrollPane(projectTree, 0)

    init {
        with(project.messageBus.connect()) {
            subscribe(MoveProjectsService.MOVE_PROJECTS_TOPIC, MoveProjectsListener { _, projects ->
                invokeLater {
                    projectStructure.reloadTreeModelAsync(projects.toList())
                }
            })
        }
        invokeLater {
            val moveProjects = project.moveProjects.allProjects.toList()
            projectStructure.reloadTreeModelAsync(moveProjects)
        }
    }

    companion object {
        private val LOG: Logger = logger<AptosToolWindow>()

        @JvmStatic
        val SELECTED_MOVE_PROJECT: DataKey<MoveProject> = DataKey.create("SELECTED_MOVE_PROJECT")

        const val APTOS_TOOLBAR_PLACE: String = "Aptos Toolbar"

//        private const val ID: String = "Aptos"

//        fun initializeToolWindow(project: Project) {
//            try {
//                val manager = ToolWindowManager.getInstance(project) as? ToolWindowManagerEx ?: return
//                val bean = ToolWindowEP.EP_NAME.extensionList.find { it.id == ID }
//                if (bean != null) {
//                    @Suppress("DEPRECATION")
//                    manager.initToolWindow(bean)
//                }
//            } catch (e: Exception) {
//                LOG.error("Unable to initialize $ID tool window", e)
//            }
//        }

//        fun isRegistered(project: Project): Boolean {
//            val manager = ToolWindowManager.getInstance(project)
//            return manager.getToolWindow(ID) != null
//        }
    }
}
