package org.move.cli.runconfig

import com.intellij.execution.Executor
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

class MoveRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : LocatableConfigurationBase<RunProfileState>(project, factory, "Move"),
    RunConfigurationWithSuppressedDefaultDebugAction {

    var cmd = MoveCommandLine("", project.contentRoots.first().toNioPathOrNull())

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
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val command = element.readString("command") ?: return
        val path = element.readPath("workingDirectory") ?: return
        this.cmd = MoveCommandLine(command, path)
    }
}
