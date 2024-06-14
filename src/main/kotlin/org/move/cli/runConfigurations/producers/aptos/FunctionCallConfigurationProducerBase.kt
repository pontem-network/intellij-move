package org.move.cli.runConfigurations.producers.aptos

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.impl.RunDialog
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationBase
import org.move.cli.runConfigurations.producers.CommandConfigurationProducerBase

abstract class FunctionCallConfigurationProducerBase<T: FunctionCallConfigurationBase>:
    CommandConfigurationProducerBase() {

    override fun onFirstRun(
        configuration: ConfigurationFromContext,
        context: ConfigurationContext,
        startRunnable: Runnable
    ) {
        @Suppress("UNCHECKED_CAST")
        val functionCallConfiguration = configuration.configuration as T

        val openEditor = functionCallConfiguration.firstRunShouldOpenEditor()
        if (openEditor) {
            val ok =
                RunDialog.editConfiguration(
                    context.project,
                    configuration.configurationSettings,
                    "Edit Function Parameters"
                )
            if (!ok) {
                return
            }
        }
        super.onFirstRun(configuration, context, startRunnable)
    }
}
