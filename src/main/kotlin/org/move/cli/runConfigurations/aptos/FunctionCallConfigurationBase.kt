package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.settings.aptosExecPath
import java.nio.file.Path

abstract class FunctionCallConfigurationBase(
    project: Project,
    factory: ConfigurationFactory,
    val configurationHandler: CommandConfigurationHandler,
): CommandConfigurationBase(project, factory) {

    override fun getCliPath(project: Project): Path? = project.aptosExecPath

    fun firstRunShouldOpenEditor(): Boolean {
        val moveProject = workingDirectory
            ?.let { wdir -> project.moveProjectsService.findMoveProjectForPath(wdir) } ?: return true
        val (_, functionCall) = configurationHandler
            .parseCommand(moveProject, command).unwrapOrNull() ?: return true
        return functionCall.parametersRequired()
    }
}
