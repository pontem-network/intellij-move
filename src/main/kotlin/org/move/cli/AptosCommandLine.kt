package org.move.cli

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.util.text.StringUtil
import java.nio.file.Path

data class AptosCommandLine(
    val command: String,
    val workingDirectory: Path?,
    val additionalArguments: List<String> = emptyList(),
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    fun commandWithParams(): String {
        return StringUtil.join(command, *additionalArguments.toTypedArray())
    }
//    fun run(
//        moveProject: MoveProject,
//        presentableName: String = command,
//        executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
//    ) {
//        val project = moveProject.project
//        val configurationName = when {
////            project.moveProjects.allProjects.size > 1 -> "$presentableName [${cargoProject.presentableName}]"
//            else -> presentableName
//        }
//        val runManager = RunManagerEx.getInstanceEx(project)
//        val configuration = createRunConfiguration(runManager, configurationName).apply {
////            if (saveConfiguration) {
////                runManager.setTemporaryConfiguration(this)
////            }
//        }
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

//    private fun createRunConfiguration(
//        runManager: RunManagerEx,
//        name: String? = null
//    ): RunnerAndConfigurationSettings =
//        runManager.createAptosCommandRunConfiguration(this, name)
}
