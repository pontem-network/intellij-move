package org.move.cli.runConfigurations.aptos.any

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class AnyCommandConfigurationFactory(
    configurationType: ConfigurationType
) :
    ConfigurationFactory(configurationType) {

    override fun getId(): String = "AnyCommand"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return AnyCommandConfiguration(project, this)
    }
}
