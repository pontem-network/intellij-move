package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.settings.aptosPath
import java.nio.file.Path

abstract class FunctionCallConfigurationBase(
    project: Project,
    factory: ConfigurationFactory,
    val configurationHandler: CommandConfigurationHandler,
): CommandConfigurationBase(project, factory) {

    var moveProject: MoveProject?
        get() = workingDirectory?.let { project.moveProjectsService.findMoveProject(it) }
        set(value) {
            workingDirectory = value?.contentRootPath
        }

    override fun getCliPath(project: Project): Path? = project.aptosPath

    fun firstRunShouldOpenEditor(): Boolean {
        val moveProject = moveProject ?: return true
        val functionCall = configurationHandler
            .parseCommand(moveProject, command).unwrapOrNull()?.second ?: return true
        return functionCall.parametersRequired()
    }
}
