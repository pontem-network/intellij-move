package org.move.cli.runConfigurations.producers.aptos

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.view.ViewCommandConfiguration
import org.move.cli.runConfigurations.aptos.view.ViewCommandConfigurationFactory
import org.move.cli.runConfigurations.aptos.view.ViewCommandConfigurationHandler

class ViewCommandConfigurationProducer : FunctionCallConfigurationProducerBase<ViewCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        ViewCommandConfigurationFactory(AptosConfigurationType.getInstance())

    override fun configFromLocation(location: PsiElement) =
        ViewCommandConfigurationHandler().configurationFromLocation(location)
}
