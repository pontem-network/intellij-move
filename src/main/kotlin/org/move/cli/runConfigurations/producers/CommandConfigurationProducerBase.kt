package org.move.cli.runConfigurations.producers

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.cli.runConfigurations.aptos.CommandConfigurationBase
import org.move.cli.settings.moveSettings

abstract class CommandConfigurationProducerBase :
    LazyRunConfigurationProducer<CommandConfigurationBase>() {

    data class CommandLineFromContext(
        val sourceElement: PsiElement,
        val configurationName: String,
        val commandLine: AptosCommandLine
    )

    override fun setupConfigurationFromContext(
        templateConfiguration: CommandConfigurationBase,
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
        configuration: CommandConfigurationBase,
        context: ConfigurationContext
    ): Boolean {
        val location = context.psiLocation ?: return false
        val cmdConf = configFromLocation(location) ?: return false
        return configuration.name == cmdConf.configurationName
                && configuration.command == cmdConf.commandLine.joinedCommand()
                && configuration.workingDirectory == cmdConf.commandLine.workingDirectory
                && configuration.environmentVariables == cmdConf.commandLine.environmentVariables
    }

    abstract fun configFromLocation(location: PsiElement): CommandLineFromContext?

    companion object {
        inline fun <reified T : PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
            if (base is T) return base
            if (!climbUp) return null
            return base.parentOfType(withSelf = false)
        }
    }
}
