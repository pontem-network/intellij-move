package org.move.cli.runConfigurations.producers.aptos

import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfigurationFactory
import org.move.cli.runConfigurations.producers.TestCommandConfigurationProducerBase
import org.move.cli.settings.Blockchain

class AptosTestCommandConfigurationProducer: TestCommandConfigurationProducerBase(Blockchain.APTOS) {

    override fun getConfigurationFactory() =
        AnyCommandConfigurationFactory(AptosConfigurationType.getInstance())
}
