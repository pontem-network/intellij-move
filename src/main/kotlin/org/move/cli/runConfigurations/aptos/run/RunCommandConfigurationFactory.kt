package org.move.cli.runConfigurations.aptos.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.aptos.workingDirectory

class RunCommandConfigurationFactory(
    configurationType: ConfigurationType
) : ConfigurationFactory(configurationType) {

    override fun getId(): String = "RunCommand"

    override fun getName(): String = "run"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        val templateConfiguration = RunCommandConfiguration(project, this)
        templateConfiguration.workingDirectory =
            project.moveProjectsService.allProjects.firstOrNull()?.workingDirectory
        return templateConfiguration
    }
}
