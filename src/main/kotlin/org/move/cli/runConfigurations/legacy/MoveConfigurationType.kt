package org.move.cli.runConfigurations.legacy

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.move.ide.MoveIcons

class MoveConfigurationType :
    SimpleConfigurationType(
        "MoveRunConfiguration",
        "Move",
        "Move command execution",
        NotNullLazyValue.createConstantValue(MoveIcons.MOVE_LOGO)
    ) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return MoveCommandConfiguration(project, this)
    }

    val factory: ConfigurationFactory get() = configurationFactories.single()

    companion object {
        fun getInstance() =
            ConfigurationTypeUtil.findConfigurationType(MoveConfigurationType::class.java)
    }
}
