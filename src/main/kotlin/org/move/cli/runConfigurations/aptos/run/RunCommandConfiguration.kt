package org.move.cli.runConfigurations.aptos.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.moveProjects
import org.move.cli.runConfigurations.aptos.CommandConfigurationBase
import org.move.cli.runConfigurations.aptos.FunctionCall
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationBase

class RunCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory
) : FunctionCallConfigurationBase(project, factory) {

    init {
        workingDirectory = if (!project.isDefault) {
            project.moveProjects.allProjects.firstOrNull()?.contentRootPath
        } else {
            null
        }
    }

    override fun getConfigurationEditor(): RunCommandConfigurationEditor {
        return RunCommandConfigurationEditor(project, command, workingDirectory)
    }
}
