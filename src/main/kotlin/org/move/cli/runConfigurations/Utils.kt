package org.move.cli.runConfigurations

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.moveProjectsService

val Project.hasMoveProject: Boolean get() = moveProjectsService.allProjects.isNotEmpty()

fun getAppropriateMoveProject(dataContext: DataContext): MoveProject? {
    val moveProjectsService = dataContext.getData(CommonDataKeys.PROJECT)?.moveProjectsService ?: return null
    moveProjectsService.allProjects.singleOrNull()?.let { return it }

    dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
        ?.let { moveProjectsService.findMoveProjectForFile(it) }
        ?.let { return it }

//    return dataContext.getData(AptosToolWindow.SELECTED_CARGO_PROJECT)
//        ?: moveProjectsService.allProjects.firstOrNull()
    return moveProjectsService.allProjects.firstOrNull()
}
