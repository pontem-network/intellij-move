package org.move.cli.runConfigurations.producers.sui

import com.intellij.execution.configurations.ConfigurationFactory
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfigurationFactory
import org.move.cli.runConfigurations.producers.TestCommandConfigurationProducerBase
import org.move.cli.runConfigurations.sui.SuiConfigurationType
import org.move.cli.settings.Blockchain

class SuiTestCommandConfigurationProducer: TestCommandConfigurationProducerBase(Blockchain.SUI) {

    override fun getConfigurationFactory(): ConfigurationFactory = SuiConfigurationType()
}