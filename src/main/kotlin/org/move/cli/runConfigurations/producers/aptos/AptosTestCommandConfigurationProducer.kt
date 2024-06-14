package org.move.cli.runConfigurations.producers.aptos

import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfigurationFactory
import org.move.cli.runConfigurations.producers.TestCommandConfigurationProducerBase

class AptosTestCommandConfigurationProducer: TestCommandConfigurationProducerBase() {

    override fun getConfigurationFactory() =
        AnyCommandConfigurationFactory(AptosConfigurationType.getInstance())
}
