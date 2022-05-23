package org.move.cli.runconfig

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jdom.Element
import org.move.cli.*
import org.move.cli.settings.aptosPath
import java.nio.file.Path

class AptosCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : LocatableConfigurationBase<RunProfileState>(project, factory, "Move"),
    RunConfigurationWithSuppressedDefaultDebugAction {

    var command: String = "move compile"
    var workingDirectory: Path? = if (!project.isDefault) {
        project.projectsService.allProjects.firstOrNull()?.rootPath
    } else {
        null
    }
    var environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    override fun getConfigurationEditor() = MoveRunConfigurationEditor()

    override fun getState(executor: Executor, execEnvironment: ExecutionEnvironment): RunProfileState {
        val projectRoot = this.workingDirectory!!
        val exePath = project.aptosPath?.toString().orEmpty()
        return AptosCommandLineState(
            execEnvironment,
            exePath,
            this.command,
            this.workingDirectory,
            environmentVariables
        )
            .apply { addConsoleFilters(MoveFileHyperlinkFilter(project, projectRoot)) }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("command", this.command)
        element.writePath("workingDirectory", this.workingDirectory)
        environmentVariables.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        this.command = element.readString("command") ?: return
        this.workingDirectory = element.readPath("workingDirectory") ?: return
        this.environmentVariables = EnvironmentVariablesData.readExternal(element)
    }
}
