package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.lang.core.psi.MvFunction

abstract class FunctionCallConfigurationBase(
    project: Project,
    factory: ConfigurationFactory
) : CommandConfigurationBase(project, factory) {

    var moveProject: MoveProject?
        get() = workingDirectory?.let { project.moveProjects.findMoveProject(it) }
        set(value) {
            workingDirectory = value?.contentRootPath
        }

    fun functionCall(): FunctionCall? {
        val moveProj = moveProject ?: return null
        return FunctionCall.parseFromCommand(moveProj, command, this::getFunction)
    }

    abstract fun getFunction(moveProject: MoveProject, functionId: String): MvFunction?
}
