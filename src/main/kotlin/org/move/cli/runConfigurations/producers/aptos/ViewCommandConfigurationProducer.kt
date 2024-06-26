package org.move.cli.runConfigurations.producers.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.aptos.AptosTransactionConfigurationType
import org.move.cli.runConfigurations.aptos.view.ViewCommandConfiguration
import org.move.cli.runConfigurations.aptos.view.ViewCommandConfigurationFactory
import org.move.cli.runConfigurations.aptos.view.ViewCommandConfigurationHandler
import org.move.cli.runConfigurations.producers.CommandLineArgsFromContext

class ViewCommandConfigurationProducer : FunctionCallConfigurationProducerBase<ViewCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        ViewCommandConfigurationFactory(AptosTransactionConfigurationType.getInstance())

    override fun fromLocation(location: PsiElement, climbUp: Boolean): CommandLineArgsFromContext? =
        ViewCommandConfigurationHandler().configurationFromLocation(location)
}
