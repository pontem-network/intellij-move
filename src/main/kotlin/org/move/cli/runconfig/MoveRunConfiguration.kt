package org.move.cli.runconfig

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jdom.Element
import org.move.cli.*
import org.move.ide.MvIcons
import org.move.lang.toNioPathOrNull
import org.move.openapiext.contentRoots
import org.move.settings.moveExecutablePathValue

class MoveRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : LocatableConfigurationBase<RunProfileState>(project, factory, "Move"),
    RunConfigurationWithSuppressedDefaultDebugAction {

    var cmd = MoveCommandLine("", project.contentRoots.firstOrNull()?.toNioPathOrNull())
    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    override fun getConfigurationEditor() = MoveRunConfigurationEditor()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val projectRoot = this.cmd.workingDirectory!!
        val executable = project.moveExecutablePathValue
        return MoveCommandLineState(environment, executable, this.cmd)
            .apply { addConsoleFilters(MoveFileHyperlinkFilter(project, projectRoot)) }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("command", this.cmd.command)
        element.writePath("workingDirectory", this.cmd.workingDirectory)
        env.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val command = element.readString("command") ?: return
        val path = element.readPath("workingDirectory") ?: return
        this.cmd = MoveCommandLine(command, path)
        env = EnvironmentVariablesData.readExternal(element)
    }
}
