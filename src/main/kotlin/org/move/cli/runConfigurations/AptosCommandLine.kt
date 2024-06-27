package org.move.cli.runConfigurations

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.text.StringUtil
import java.nio.file.Path

data class AptosCommandLine(
    val subCommand: String?,
    val arguments: List<String> = emptyList(),
    val workingDirectory: Path? = null,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    fun joinArgs(): String {
        return StringUtil.join(subCommand?.split(" ").orEmpty() + arguments, " ")
    }

    fun toGeneralCommandLine(cliExePath: Path): GeneralCommandLine {
        val generalCommandLine = GeneralCommandLine()
            .withExePath(cliExePath.toString())
            // subcommand can be null
            .withParameters(subCommand?.split(" ").orEmpty())
            .withParameters(this.arguments)
            .withWorkDirectory(this.workingDirectory?.toString())
            .withCharset(Charsets.UTF_8)
        this.environmentVariables.configureCommandLine(generalCommandLine, true)
        return generalCommandLine
    }

//    fun createRunConfiguration(
//        moveProject: MoveProject,
//        configurationName: String,
//        save: Boolean
//    ): RunnerAndConfigurationSettings {
//        val project = moveProject.project
//        val runConfiguration =
//            AnyCommandConfigurationFactory.createTemplateRunConfiguration(
//                project,
//                configurationName,
//                save = save
//            )
//        val anyCommandConfiguration = runConfiguration.configuration as AnyCommandConfiguration
//        anyCommandConfiguration.command = this.joinArgs()
//        anyCommandConfiguration.workingDirectory = this.workingDirectory
//        anyCommandConfiguration.environmentVariables = this.environmentVariables
//        return runConfiguration
//    }
}
