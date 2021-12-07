package org.move.cli

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths

abstract class SubcommandRunConfigurationBase(
    name: String,
    project: Project,
    factory: ConfigurationFactory,
) : RunConfigurationBase<RunProfileState>(project, factory, name) {

    var command: String = ""
    var workingDirectory: Path? = project.basePath?.let { Paths.get(it) }

    abstract fun pathToExecutable(): String

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val projectRoot = this.workingDirectory!!
        return MvCommandLineState(environment, this)
            .apply { addConsoleFilters(MvFileHyperlinkFilter(project, projectRoot)) }
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
