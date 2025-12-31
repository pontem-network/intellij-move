package org.move.cli.runConfigurations.endless

import com.intellij.execution.configurations.ConfigurationTypeBase
import javax.swing.Icon

abstract class EndlessConfigurationTypeBase(
    id: String,
    displayName: String,
    description: String?,
    icon: Icon?
): ConfigurationTypeBase(id, displayName, description, icon)