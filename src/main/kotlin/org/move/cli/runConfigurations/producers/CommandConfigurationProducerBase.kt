package org.move.cli.runConfigurations.producers

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.move.cli.runConfigurations.aptos.CommandConfigurationBase
import org.move.cli.settings.moveSettings

abstract class CommandConfigurationProducerBase :
    LazyRunConfigurationProducer<CommandConfigurationBase>() {

    override fun setupConfigurationFromContext(
        templateConfiguration: CommandConfigurationBase,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val cmdConf = configFromLocation(sourceElement.get()) ?: return false
        templateConfiguration.name = cmdConf.configurationName

        val commandLine = cmdConf.commandLineArgs
        templateConfiguration.command = commandLine.joinArgs()
        templateConfiguration.workingDirectory = commandLine.workingDirectory

        var envVars = commandLine.environmentVariables
        if (templateConfiguration.project.moveSettings.state.disableTelemetry) {
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
                && configuration.command == cmdConf.commandLineArgs.joinArgs()
                && configuration.workingDirectory == cmdConf.commandLineArgs.workingDirectory
                && configuration.environmentVariables == cmdConf.commandLineArgs.environmentVariables
    }

    abstract fun configFromLocation(location: PsiElement): CommandLineArgsFromContext?

    companion object {
        inline fun <reified T : PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
            if (base is T) return base
            if (!climbUp) return null
            return base.parentOfType(withSelf = false)
        }
    }
}
