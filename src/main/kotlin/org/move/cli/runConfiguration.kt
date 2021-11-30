package org.move.cli

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.move.ide.MoveIcons
import org.move.settings.moveExecutablePathValue
import javax.swing.Icon

class MoveRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Move"
    override fun getConfigurationTypeDescription(): String = "Move command execution"
    override fun getIcon(): Icon = MoveIcons.MOVE
    override fun getId(): String = "MoveRunConfiguration"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(MoveRunConfigurationFactory(this))
    }
}

class MoveRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

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
