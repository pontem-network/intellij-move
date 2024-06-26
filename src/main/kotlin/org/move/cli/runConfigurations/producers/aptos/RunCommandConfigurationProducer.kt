package org.move.cli.runConfigurations.producers.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.aptos.AptosTransactionConfigurationType
import org.move.cli.runConfigurations.aptos.run.RunCommandConfiguration
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationFactory
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationHandler
import org.move.cli.runConfigurations.producers.CommandLineArgsFromContext

class RunCommandConfigurationProducer : FunctionCallConfigurationProducerBase<RunCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        RunCommandConfigurationFactory(AptosTransactionConfigurationType.getInstance())

    override fun fromLocation(location: PsiElement, climbUp: Boolean): CommandLineArgsFromContext? =
        RunCommandConfigurationHandler().configurationFromLocation(location)
}
