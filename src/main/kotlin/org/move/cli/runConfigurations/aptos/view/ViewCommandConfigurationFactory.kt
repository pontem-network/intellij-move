package org.move.cli.runConfigurations.aptos.view

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.aptos.workingDirectory

class ViewCommandConfigurationFactory(
    configurationType: ConfigurationType
) : ConfigurationFactory(configurationType) {

    override fun getId(): String = "ViewCommand"

    override fun getName(): String = "view"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        val templateConfiguration = ViewCommandConfiguration(project, this)
        templateConfiguration.workingDirectory =
            project.moveProjectsService.allProjects.firstOrNull()?.workingDirectory
        return templateConfiguration
    }
}
