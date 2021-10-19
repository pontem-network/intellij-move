package org.move.cli.dove

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.move.cli.SubcommandRunConfigurationBase
import org.move.ide.MoveIcons
import org.move.settings.dovePathValue
import javax.swing.Icon

class DoveRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Dove"
    override fun getConfigurationTypeDescription(): String = "Dove command execution"
    override fun getIcon(): Icon = MoveIcons.MOVE
    override fun getId(): String = "DoveRunConfiguration"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(DoveRunConfigurationFactory(this))
    }
}

class DoveRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return DoveRunConfiguration(project, this)
    }
}

class DoveRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : SubcommandRunConfigurationBase("Dove", project, factory) {

    override fun pathToExecutable(): String = project.dovePathValue

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DoveExecutableSettingsEditor()
    }
}
