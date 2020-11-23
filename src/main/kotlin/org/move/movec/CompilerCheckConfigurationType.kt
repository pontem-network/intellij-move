package org.move.movec

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

class CompilerCheckConfigurationType : ConfigurationTypeBase(
    "CompilerCheckConfiguration",
    "move-tools Compiler Check",
    "Check project files with move-tools compiler",
    AllIcons.RunConfigurations.Application
) {
    init {
        addFactory(CompilerCheckConfigurationFactory(this))
    }
}


class CompilerCheckConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "Move-tools Compiler Check"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        CompilerCheckConfiguration(project, "Demo", this)

}