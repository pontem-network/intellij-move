package org.move.cli.runConfigurations.producers

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.impl.RunDialog
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationBase

abstract class FunctionCallConfigurationProducerBase<T : FunctionCallConfigurationBase> :
    CommandConfigurationProducerBase() {

    override fun onFirstRun(
        configuration: ConfigurationFromContext,
        context: ConfigurationContext,
        startRunnable: Runnable
    ) {
        @Suppress("UNCHECKED_CAST")
        val runCommandConfiguration = configuration.configuration as T
        val configMissing =
            runCommandConfiguration.functionCall()?.hasRequiredParameters() ?: true
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
}
