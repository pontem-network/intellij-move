package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.moveProjects
import org.move.lang.core.psi.MvFunction

abstract class FunctionCallConfigurationBase(
    project: Project,
    factory: ConfigurationFactory
) : CommandConfigurationBase(project, factory) {

    init {
        workingDirectory = if (!project.isDefault) {
            project.moveProjects.allProjects.firstOrNull()?.contentRootPath
        } else {
            null
        }
    }

    fun functionCall(): FunctionCall? {
        return FunctionCall.parseFromCommand(project, command, workingDirectory, this::getFunction)
    }

    abstract fun getFunction(project: Project, functionId: String): MvFunction?
}
