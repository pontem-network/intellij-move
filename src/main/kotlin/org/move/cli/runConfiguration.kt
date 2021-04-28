package org.move.cli

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element
import org.move.ide.MoveIcons
import java.nio.file.Path
import java.nio.file.Paths
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
) : RunConfigurationBase<RunProfileState>(project, factory, "Dove") {

    var command: String = ""
    var workingDirectory: Path? = project.basePath?.let { Paths.get(it) }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val projectRoot = this.workingDirectory!!
        return DoveCommandLineState(environment, this)
            .apply { addConsoleFilters(MoveFileHyperlinkFilter(project, projectRoot)) }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DoveExecutableSettingsEditor()
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("command", command)
        element.writePath("workingDirectory", workingDirectory)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("command")?.let { command = it }
        element.readPath("workingDirectory")?.let { workingDirectory = it }
    }
}
