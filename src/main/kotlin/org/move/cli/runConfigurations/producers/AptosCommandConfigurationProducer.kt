package org.move.cli.runConfigurations.producers

import com.intellij.execution.*
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.Aptos
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfiguration
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfigurationFactory
import org.move.cli.runConfigurations.legacy.MoveCommandConfiguration
import org.move.cli.runConfigurations.legacy.MoveConfigurationType
import org.move.cli.settings.moveSettings
import org.move.ide.notifications.MvNotifications

fun Aptos.CommandLine.createRunConfigurationAndRun(
    moveProject: MoveProject,
    presentableName: String = subCommand ?: "unknown",
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
                "${executor.actionName} action is not available for `$executableName $subCommand`",
                NotificationType.WARNING
            )
            .notify(project)
        DefaultRunExecutor.getRunExecutorInstance()
    } else {
        executor
    }

    ProgramRunnerUtil.executeConfiguration(configuration, finalExecutor)
}

fun RunManager.createAptosCommandRunConfiguration(
    commandLine: Aptos.CommandLine,
    name: String? = null
): RunnerAndConfigurationSettings {
    val runnerAndConfigurationSettings = createConfiguration(
        name ?: commandLine.subCommand ?: "unknown",
        MoveConfigurationType.getInstance().factory
    )
    val configuration = runnerAndConfigurationSettings.configuration as MoveCommandConfiguration

    configuration.command = commandLine.joinedCommand()
    configuration.workingDirectory = commandLine.workingDirectory
    configuration.environmentVariables = commandLine.environmentVariables

    return runnerAndConfigurationSettings
}


data class AptosCommandLineFromContext(
    val sourceElement: PsiElement,
    val configurationName: String,
    val commandLine: Aptos.CommandLine
) {
    fun createRunConfigurationAndRun(moveProject: MoveProject) {
        commandLine.createRunConfigurationAndRun(
            moveProject,
            configurationName,
            saveConfiguration = true,
        )
    }
}

abstract class AptosCommandConfigurationProducer :
    LazyRunConfigurationProducer<AnyCommandConfiguration>() {

    override fun getConfigurationFactory() =
        AnyCommandConfigurationFactory(AptosConfigurationType.getInstance())

    override fun setupConfigurationFromContext(
        templateConfiguration: AnyCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val cmdConf = configFromLocation(sourceElement.get()) ?: return false
        templateConfiguration.name = cmdConf.configurationName

        val commandLine = cmdConf.commandLine
        templateConfiguration.command = commandLine.joinedCommand()
        templateConfiguration.workingDirectory = commandLine.workingDirectory

        var envVars = commandLine.environmentVariables
        if (templateConfiguration.project.moveSettings.settingsState.disableTelemetry) {
            envVars = envVars.with(mapOf("APTOS_DISABLE_TELEMETRY" to "true"))
        }
        templateConfiguration.environmentVariables = envVars
        return true
    }

    override fun isConfigurationFromContext(
        configuration: AnyCommandConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val location = context.psiLocation ?: return false
        val cmdConf = configFromLocation(location) ?: return false
        return configuration.name == cmdConf.configurationName
                && configuration.command == cmdConf.commandLine.joinedCommand()
                && configuration.workingDirectory == cmdConf.commandLine.workingDirectory
                && configuration.environmentVariables == cmdConf.commandLine.environmentVariables
    }

    abstract fun configFromLocation(location: PsiElement): AptosCommandLineFromContext?

    companion object {
        inline fun <reified T : PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
            if (base is T) return base
            if (!climbUp) return null
            return base.parentOfType(withSelf = false)
        }
    }
}
