package org.move.cli.runconfig

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType


abstract class MoveBinaryRunConfigurationProducer :
    LazyRunConfigurationProducer<AptosCommandRunConfiguration>() {

    override fun getConfigurationFactory() = AptosCommandConfigurationType.getInstance()

    override fun setupConfigurationFromContext(
        templateConfiguration: AptosCommandRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val cmdConf = configFromLocation(sourceElement.get()) ?: return false
        templateConfiguration.name = cmdConf.configurationName

        val commandLine = cmdConf.commandLine
        templateConfiguration.command = commandLine.commandWithParams()
        templateConfiguration.workingDirectory = commandLine.workingDirectory
        templateConfiguration.environmentVariables = commandLine.environmentVariables
        return true
    }

    override fun isConfigurationFromContext(
        configuration: AptosCommandRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val location = context.psiLocation ?: return false
        val cmdConf = configFromLocation(location) ?: return false
        return configuration.name == cmdConf.configurationName
                && configuration.command == cmdConf.commandLine.commandWithParams()
                && configuration.workingDirectory == cmdConf.commandLine.workingDirectory
                && configuration.environmentVariables == cmdConf.commandLine.environmentVariables
    }

    abstract fun configFromLocation(location: PsiElement): AptosCommandConfig?

    companion object {
        inline fun <reified T : PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
            if (base is T) return base
            if (!climbUp) return null
            return base.parentOfType(withSelf = false)
        }
    }
}
