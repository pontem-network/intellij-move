package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.NotNullLazyValue
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationFactory
import org.move.cli.runConfigurations.aptos.view.ViewCommandConfigurationFactory
import org.move.ide.MoveIcons

class AptosTransactionConfigurationType :
    AptosConfigurationTypeBase(
        "AptosTransactionConfiguration",
        "Aptos Transaction",
        "Aptos transaction execution",
        MoveIcons.APTOS_LOGO
    ) {

    init {
        addFactory(RunCommandConfigurationFactory(this))
        addFactory(ViewCommandConfigurationFactory(this))
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        fun getInstance(): AptosTransactionConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(AptosTransactionConfigurationType::class.java)
        }
    }
}
