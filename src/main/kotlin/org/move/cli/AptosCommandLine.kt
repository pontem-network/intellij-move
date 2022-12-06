package org.move.cli

import com.intellij.execution.*
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.text.StringUtil
import org.move.cli.runconfig.AptosCommandConfiguration
import org.move.cli.runconfig.AptosCommandConfigurationType
import org.move.ide.notifications.MvNotifications
import java.nio.file.Path

fun RunManager.createAptosCommandRunConfiguration(
    aptosCommandLine: AptosCommandLine,
    name: String? = null
): RunnerAndConfigurationSettings {
    val runnerAndConfigurationSettings = createConfiguration(
        name ?: aptosCommandLine.command,
        AptosCommandConfigurationType.getInstance().factory
    )
    val configuration = runnerAndConfigurationSettings.configuration as AptosCommandConfiguration
    configuration.setFromCmd(aptosCommandLine)
    return runnerAndConfigurationSettings
}

data class AptosCommandLine(
    val command: String,
    val workingDirectory: Path?,
    val additionalArguments: List<String> = emptyList(),
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    fun commandWithParams(): String {
        return StringUtil.join(listOf(command, *additionalArguments.toTypedArray()), " ")
    }

    fun run(
        moveProject: MoveProject,
        presentableName: String = command,
        saveConfiguration: Boolean = true,
        executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
    ) {
        val project = moveProject.project
        val configurationName = when {
//            project.moveProjects.allProjects.size > 1 -> "$presentableName [${cargoProject.presentableName}]"
            else -> presentableName
        }
        val runManager = RunManagerEx.getInstanceEx(project)
        val configuration = runManager
            .createAptosCommandRunConfiguration(this, configurationName)
            .apply {
                if (saveConfiguration) {
                    runManager.setTemporaryConfiguration(this)
                }
            }

        val runner = ProgramRunner.getRunner(executor.id, configuration.configuration)
        val executableName = "aptos"
        val finalExecutor = if (runner == null) {
            MvNotifications.pluginNotifications()
                .createNotification(
                    "${executor.actionName} action is not available for `$executableName $command`",
                    NotificationType.WARNING
                )
                .notify(project)
            DefaultRunExecutor.getRunExecutorInstance()
        } else {
            executor
        }

        ProgramRunnerUtil.executeConfiguration(configuration, finalExecutor)
    }
}
