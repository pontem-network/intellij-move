package org.move.cli.runConfigurations.producers

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.settings.moveSettings

abstract class CommandConfigurationProducerBase:
    LazyRunConfigurationProducer<CommandConfigurationBase>() {

    fun configFromLocation(location: PsiElement, climbUp: Boolean = true): EndlessCommandLineFromContext? =
        fromLocation(location, climbUp)

    abstract fun fromLocation(location: PsiElement, climbUp: Boolean = true): EndlessCommandLineFromContext?

    override fun setupConfigurationFromContext(
        templateConfiguration: CommandConfigurationBase,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val cmdConf = configFromLocation(sourceElement.get()) ?: return false
        templateConfiguration.name = cmdConf.configurationName

        val commandLine = cmdConf.commandLine
        templateConfiguration.command = commandLine.commandLineString
        templateConfiguration.workingDirectory = commandLine.workingDirectory

        var environment = commandLine.environmentVariables.envs
        if (context.project.moveSettings.disableTelemetry) {
            environment = environment + mapOf("ENDLESS_DISABLE_TELEMETRY" to "true")
        }
        templateConfiguration.environmentVariables = EnvironmentVariablesData.create(environment, true)
        return true

    }

    override fun isConfigurationFromContext(
        configuration: CommandConfigurationBase,
        context: ConfigurationContext
    ): Boolean {
        val location = context.psiLocation ?: return false
        val cmdConf = configFromLocation(location) ?: return false
        return configuration.name == cmdConf.configurationName
                && configuration.command == cmdConf.commandLine.commandLineString
                && configuration.workingDirectory == cmdConf.commandLine.workingDirectory
                && configuration.environmentVariables == cmdConf.commandLine.environmentVariables
    }

    companion object {
        inline fun <reified T: PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
            if (base is T) return base
            if (!climbUp) return null
            return base.parentOfType(withSelf = false)
        }
    }
}
