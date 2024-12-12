package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.ConfigurationTypeBase
import javax.swing.Icon

abstract class AptosConfigurationTypeBase(
    id: String,
    displayName: String,
    description: String?,
    icon: Icon?
): ConfigurationTypeBase(id, displayName, description, icon)