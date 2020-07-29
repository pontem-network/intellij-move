package org.move.move_tools

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class CompilerCheckConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : RunConfigurationBase<RunProfileState>(project, factory, name) {
    var senderAddress: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return CompilerCheckEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val command = CompilerCommand("sender address is: $senderAddress")
        return CompilerCheckState(environment, this, command)
    }

}
