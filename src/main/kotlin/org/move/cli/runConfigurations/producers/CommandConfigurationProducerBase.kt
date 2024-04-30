package org.move.cli.runConfigurations.producers

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.settings.Blockchain
import org.move.cli.settings.moveSettings

abstract class CommandConfigurationProducerBase(val blockchain: Blockchain):
    LazyRunConfigurationProducer<CommandConfigurationBase>() {

    fun configFromLocation(location: PsiElement, climbUp: Boolean = true): CommandLineArgsFromContext? {
        val project = location.project
        if (project.moveSettings.blockchain != blockchain) {
            return null
        }
        return fromLocation(location, climbUp)
    }

    abstract fun fromLocation(location: PsiElement, climbUp: Boolean = true): CommandLineArgsFromContext?

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
        if (blockchain == Blockchain.APTOS
            && context.project.moveSettings.disableTelemetry
        ) {
            envVars = envVars.with(mapOf("APTOS_DISABLE_TELEMETRY" to "true"))
        }
        templateConfiguration.environmentVariables = envVars
        return true
    }

    override fun isConfigurationFromContext(
        configuration: CommandConfigurationBase,
        context: ConfigurationContext
    ): Boolean {
        val project = context.project
        if (project.moveSettings.blockchain != blockchain) {
            return false
        }
        val location = context.psiLocation ?: return false
        val cmdConf = configFromLocation(location) ?: return false
        return configuration.name == cmdConf.configurationName
                && configuration.command == cmdConf.commandLineArgs.joinArgs()
                && configuration.workingDirectory == cmdConf.commandLineArgs.workingDirectory
                && configuration.environmentVariables == cmdConf.commandLineArgs.environmentVariables
    }

    companion object {
        inline fun <reified T: PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
            if (base is T) return base
            if (!climbUp) return null
            return base.parentOfType(withSelf = false)
        }
    }
}
