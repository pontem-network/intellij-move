package org.move.cli.runConfigurations.producers

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.run.RunCommandConfiguration
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationFactory
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationHandler

class RunCommandConfigurationProducer : FunctionCallConfigurationProducerBase<RunCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        RunCommandConfigurationFactory(AptosConfigurationType.getInstance())

    override fun configFromLocation(location: PsiElement) =
        RunCommandConfigurationHandler().configurationFromLocation(location)
}
