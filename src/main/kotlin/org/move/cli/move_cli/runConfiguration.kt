package org.move.cli.move_cli

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.move.cli.SubcommandRunConfigurationBase
import org.move.ide.MoveIcons
import org.move.settings.dovePathValue
import org.move.settings.moveCLIPathValue
import javax.swing.Icon

class MoveCLIRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Move CLI"
    override fun getConfigurationTypeDescription(): String = "Move CLI command execution"
    override fun getIcon(): Icon = MoveIcons.MOVE
    override fun getId(): String = "MoveCLIRunConfiguration"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(MoveCLIRunConfigurationFactory(this))
    }
}

class MoveCLIRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return MoveCLIRunConfiguration(project, this)
    }
}

class MoveCLIRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : SubcommandRunConfigurationBase("Move CLI", project, factory) {

    override fun pathToExecutable(): String = project.moveCLIPathValue

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return MoveCLIExecutableSettingsEditor()
    }
}
