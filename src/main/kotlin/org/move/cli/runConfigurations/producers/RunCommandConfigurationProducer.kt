package org.move.cli.runConfigurations.producers

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.impl.RunDialog
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.run.RunCommandConfiguration
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationFactory
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.isEntry
import org.move.lang.core.types.ItemQualName
import org.move.lang.moveProject

class RunCommandConfigurationProducer : CommandConfigurationProducerBase() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        RunCommandConfigurationFactory(AptosConfigurationType.getInstance())

    override fun configFromLocation(location: PsiElement) = fromLocation(location)

    override fun onFirstRun(
        configuration: ConfigurationFromContext,
        context: ConfigurationContext,
        startRunnable: Runnable
    ) {
        val runCommandConfiguration = configuration.configuration as RunCommandConfiguration
        val configMissing =
            runCommandConfiguration.getTransaction()?.hasRequiredParameters() ?: true
        if (configMissing) {
            val ok =
                RunDialog.editConfiguration(
                    context.project,
                    configuration.configurationSettings,
                    "Edit Transaction Parameters"
                )
            if (!ok) {
                return
            }
        }
        super.onFirstRun(configuration, context, startRunnable)
    }

    companion object {
        fun fromLocation(location: PsiElement): CommandLineFromContext? {
            val entryFunction =
                findElement<MvFunction>(location, true)?.takeIf { it.isEntry } ?: return null
            val functionQualName = entryFunction.qualName ?: return null

            val moveProject = entryFunction.moveProject ?: return null
            val profileName = moveProject.profiles.firstOrNull()
            val workingDirectory = moveProject.contentRootPath

            val functionId = functionQualName.cmdText()
            val arguments = mutableListOf("--function-id", functionId)
            if (profileName != null) {
                arguments.addAll(listOf("--profile", profileName))
            }
            val commandLine = AptosCommandLine("move run", arguments, workingDirectory)
            return CommandLineFromContext(
                entryFunction,
                "Run ${ItemQualName.qualNameForCompletion(functionId)}",
                commandLine
            )
        }
    }
}
