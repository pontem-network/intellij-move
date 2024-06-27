package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.CommandConfigurationBase

abstract class FunctionCallConfigurationBase(
    project: Project,
    factory: ConfigurationFactory,
    val configurationHandler: CommandConfigurationHandler,
): CommandConfigurationBase(project, factory) {

    fun firstRunShouldOpenEditor(): Boolean {
        val moveProject = workingDirectory
            ?.let { wdir -> project.moveProjectsService.findMoveProjectForPath(wdir) } ?: return true
        val (_, functionCall) = configurationHandler
            .parseTransactionCommand(moveProject, command).unwrapOrNull() ?: return true
        return functionCall.parametersRequired()
    }
}
