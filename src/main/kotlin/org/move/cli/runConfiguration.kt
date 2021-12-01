package org.move.cli

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.move.ide.MoveIcons
import org.move.settings.moveExecutablePathValue
import javax.swing.Icon

class MoveRunConfigurationType :
    SimpleConfigurationType(
        "MoveRunConfiguration",
        "Move",
        "Move command execution",
        NotNullLazyValue.createConstantValue(MoveIcons.MOVE)
    ) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return MoveRunConfiguration(project, this)
    }
}

class MoveRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : SubcommandRunConfigurationBase("Move", project, factory) {

    override fun pathToExecutable(): String = project.moveExecutablePathValue

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return MoveExecutableSettingsEditor()
    }
}
