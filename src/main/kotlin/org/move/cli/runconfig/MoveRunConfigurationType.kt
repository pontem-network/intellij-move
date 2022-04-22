package org.move.cli.runconfig

import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.move.ide.MvIcons

class MoveRunConfigurationType :
    SimpleConfigurationType(
        "MoveRunConfiguration",
        "Move",
        "Move command execution",
        NotNullLazyValue.createConstantValue(MvIcons.MOVE)
    ) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return MoveRunConfiguration(project, this)
    }

    companion object {
        fun getInstance() =
            ConfigurationTypeUtil.findConfigurationType(MoveRunConfigurationType::class.java)
    }
}
