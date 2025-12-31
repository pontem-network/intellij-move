package org.move.stdext

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.NotificationType
import org.move.ide.notifications.MvNotifications

fun RunnerAndConfigurationSettings.execute() {
    val configuration = this.configuration

    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val runner = ProgramRunner.getRunner(executor.id, configuration)
    val finalExecutor = if (runner == null) {
        val executableName = executableName("endless")
        MvNotifications.pluginNotifications()
            .createNotification(
                "${executor.actionName} action is not available for `$executableName`",
                NotificationType.WARNING
            )
            .notify(configuration.project)
        DefaultRunExecutor.getRunExecutorInstance()
    } else {
        executor
    }

    ProgramRunnerUtil.executeConfiguration(this, finalExecutor)
}
