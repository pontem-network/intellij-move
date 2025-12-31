package org.move.cli.runConfigurations.endless

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import org.move.cli.runConfigurations.endless.cmd.EndlessCommandConfigurationFactory
import org.move.ide.MoveIcons

class EndlessCommandConfigurationType:
    EndlessConfigurationTypeBase(
        "EndlessCommandConfiguration",
        "Endless",
        "Endless command execution",
        MoveIcons.ENDLESS_LOGO
    ) {
        init {
            addFactory(EndlessCommandConfigurationFactory(this))
        }

    val factory: ConfigurationFactory get() = configurationFactories.single()

    @Suppress("CompanionObjectInExtension")
    companion object {
        fun getInstance(): EndlessCommandConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(EndlessCommandConfigurationType::class.java)
        }
    }
}