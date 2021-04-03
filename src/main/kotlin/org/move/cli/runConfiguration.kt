package org.move.cli

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.move.ide.MoveIcons
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
        return DoveRunConfiguration(project, this, "Dove", "")
    }
}

class DoveRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
    var command: String,
) : RunConfigurationBase<RunProfileState>(project, factory, name) {

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return DoveCommandLineState(environment, this)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DoveExecutableSettingsEditor()
    }
}
