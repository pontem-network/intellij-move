package org.move.cli.runConfigurations.aptos

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.text.StringUtil
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfiguration
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfigurationFactory
import java.nio.file.Path

data class AptosCommandLine(
    val subCommand: String?,
    val arguments: List<String> = emptyList(),
    val workingDirectory: Path? = null,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    fun joinedCommand(): String {
        return StringUtil.join(listOfNotNull(subCommand, *arguments.toTypedArray()), " ")
    }

    fun toGeneralCommandLine(aptos: Aptos): GeneralCommandLine {
        val generalCommandLine = GeneralCommandLine()
            .withExePath(aptos.location.toString())
            // subcommand can be null
            .withParameters(listOfNotNull(subCommand))
            .withParameters(this.arguments)
            .withWorkDirectory(this.workingDirectory?.toString())
            .withCharset(Charsets.UTF_8)
        this.environmentVariables.configureCommandLine(generalCommandLine, true)
        return generalCommandLine
    }

    fun createRunConfiguration(
        moveProject: MoveProject,
        configurationName: String,
        save: Boolean
    ): RunnerAndConfigurationSettings {
        val project = moveProject.project
        val runConfiguration =
            AnyCommandConfigurationFactory.createTemplateRunConfiguration(
                project,
                configurationName,
                save = save
            )
        val anyCommandConfiguration = runConfiguration.configuration as AnyCommandConfiguration
        anyCommandConfiguration.command = this.joinedCommand()
        anyCommandConfiguration.workingDirectory = this.workingDirectory
        anyCommandConfiguration.environmentVariables = this.environmentVariables
        return runConfiguration
    }
}
