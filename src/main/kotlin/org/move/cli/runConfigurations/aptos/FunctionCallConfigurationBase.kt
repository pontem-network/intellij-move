package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.lang.core.psi.MvFunction

abstract class FunctionCallConfigurationBase(
    project: Project,
    factory: ConfigurationFactory,
    val configurationHandler: FunctionCallConfigurationHandler,
) : CommandConfigurationBase(project, factory) {

    var moveProject: MoveProject?
        get() = workingDirectory?.let { project.moveProjects.findMoveProject(it) }
        set(value) {
            workingDirectory = value?.contentRootPath
        }

    fun functionCall(): FunctionCall? {
        val moveProject = moveProject ?: return null
        return configurationHandler.parseCommand(moveProject, command).unwrapOrNull()?.second
    }
}
