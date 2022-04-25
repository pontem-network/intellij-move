package org.move.cli.runconfig

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement


abstract class MoveBinaryRunConfigurationProducer : LazyRunConfigurationProducer<MoveRunConfiguration>() {

    override fun getConfigurationFactory() = MoveRunConfigurationType.getInstance()

    override fun setupConfigurationFromContext(
        templateConfiguration: MoveRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val cmdConf = configFromLocation(sourceElement.get()) ?: return false
        templateConfiguration.name = cmdConf.name
        templateConfiguration.cmd =
            MoveCmd(cmdConf.command, cmdConf.workingDirectory, cmdConf.env)
        return true
    }

    override fun isConfigurationFromContext(
        configuration: MoveRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val location = context.psiLocation ?: return false
        val cmdConf = configFromLocation(location) ?: return false
        return configuration.name == cmdConf.name
                && configuration.cmd == cmdConf.commandLine()
    }

    abstract fun configFromLocation(location: PsiElement): MoveCmdConf?
}
