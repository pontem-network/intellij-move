package org.move.cli.runConfigurations.legacy

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.util.text.StringUtil
import java.nio.file.Path

data class MoveCommandLine(
    val command: String,
    val workingDirectory: Path?,
    val additionalArguments: List<String> = emptyList(),
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    fun commandWithParams(): String {
        return StringUtil.join(listOf(command, *additionalArguments.toTypedArray()), " ")
    }

//    fun run(
//        moveProject: MoveProject,
//        presentableName: String = command,
//        saveConfiguration: Boolean = true,
//        executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
//    ) {
//        val project = moveProject.project
//        val configurationName = when {
////            project.moveProjects.allProjects.size > 1 -> "$presentableName [${cargoProject.presentableName}]"
//            else -> presentableName
//        }
//        val runManager = RunManagerEx.getInstanceEx(project)
//        val configuration = runManager
//            .createAptosCommandRunConfiguration(this, configurationName)
//            .apply {
//                if (saveConfiguration) {
//                    runManager.setTemporaryConfiguration(this)
//                }
//            }
//
//        val runner = ProgramRunner.getRunner(executor.id, configuration.configuration)
//        val executableName = "aptos"
//        val finalExecutor = if (runner == null) {
//            MvNotifications.pluginNotifications()
//                .createNotification(
//                    "${executor.actionName} action is not available for `$executableName $command`",
//                    NotificationType.WARNING
//                )
//                .notify(project)
//            DefaultRunExecutor.getRunExecutorInstance()
//        } else {
//            executor
//        }
//
//        ProgramRunnerUtil.executeConfiguration(configuration, finalExecutor)
//    }
}
