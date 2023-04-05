package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project

abstract class FunctionCallConfigurationBase(
    project: Project,
    factory: ConfigurationFactory
): CommandConfigurationBase(project, factory) {

    fun functionCall(): FunctionCall? {
        return FunctionCall.parseFromCommand(project, command, workingDirectory)
    }


}
