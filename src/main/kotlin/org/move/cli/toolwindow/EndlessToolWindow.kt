package org.move.cli.toolwindow

import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.invokeLater
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
import org.move.cli.moveProjectsService
import javax.swing.JComponent

class EndlessToolWindowFactory: ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        if (!project.moveProjectsService.hasAtLeastOneValidProject) {
            project.moveProjectsService
                .scheduleProjectsRefresh("Endless Tool Window opened")
        }

        val toolwindowPanel = EndlessToolWindowPanel(project)
        val tab = ContentFactory.getInstance()
            .createContent(toolwindowPanel, "", false)
        toolWindow.contentManager.addContent(tab)
    }

    // TODO: isApplicable() and initializeToolWindow() cannot be copied from intellij-rust in 241,
    //       implement it instead with ExternalToolWindowManager later
}

private class EndlessToolWindowPanel(project: Project): SimpleToolWindowPanel(true, false) {
    private val endlessTab = EndlessToolWindow(project)

    init {
        toolbar = endlessTab.toolbar.component
        endlessTab.toolbar.targetComponent = this
        setContent(endlessTab.content)
    }

    @Deprecated("Migrate to [uiDataSnapshot] ASAP")
    override fun getData(dataId: String): Any? =
        @Suppress("DEPRECATION")
        when {
            EndlessToolWindow.SELECTED_MOVE_PROJECT.`is`(dataId) -> endlessTab.selectedProject
            PlatformDataKeys.TREE_EXPANDER.`is`(dataId) -> endlessTab.treeExpander
            else -> super.getData(dataId)
        }
}

class EndlessToolWindow(private val project: Project) {

    val toolbar: ActionToolbar = run {
        val actionManager = ActionManager.getInstance()
        actionManager.createActionToolbar(
            ENDLESS_TOOLBAR_PLACE,
            actionManager.getAction("Move.Endless") as DefaultActionGroup,
            true
        )
    }

    private val projectTree = MoveProjectsTree()
    private val projectStructure = MoveProjectsTreeStructure(projectTree, project)

    val treeExpander: TreeExpander = object: DefaultTreeExpander(projectTree) {
        override fun isCollapseAllVisible(): Boolean = project.hasMoveProject
        override fun isExpandAllVisible(): Boolean = project.hasMoveProject
    }

    val selectedProject: MoveProject? get() = projectTree.selectedProject

    val content: JComponent = ScrollPaneFactory.createScrollPane(projectTree, 0)

    init {
        with(project.messageBus.connect()) {
            subscribe(MoveProjectsService.MOVE_PROJECTS_TOPIC, MoveProjectsListener { _, projects ->
                invokeLater {
                    projectStructure.updateMoveProjects(projects.toList())
                }
            })
        }
        invokeLater {
            projectStructure.updateMoveProjects(project.moveProjectsService.allProjects.toList())
        }
    }

    companion object {
        @JvmStatic
        val SELECTED_MOVE_PROJECT: DataKey<MoveProject> = DataKey.create("SELECTED_MOVE_PROJECT")

        const val ENDLESS_TOOLBAR_PLACE: String = "Endless Toolbar"
    }
}
