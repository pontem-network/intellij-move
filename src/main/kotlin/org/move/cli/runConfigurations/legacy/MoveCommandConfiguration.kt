package org.move.cli.runConfigurations.legacy

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.execution.ParametersListUtil
import org.jdom.Element
import org.move.cli.*
import org.move.cli.settings.aptosPath
import org.move.stdext.exists
import java.nio.file.Path

class MoveCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : LocatableConfigurationBase<RunProfileState>(project, factory, "Move"),
    RunConfigurationWithSuppressedDefaultDebugAction {

    var command: String = "move compile"
    var workingDirectory: Path? = if (!project.isDefault) {
        project.moveProjectsService.allProjects.firstOrNull()?.contentRootPath
    } else {
        null
    }
    var environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        val config = clean()
        if (config is CleanConfiguration.Err) throw config.error
    }

    override fun getConfigurationEditor() = MoveCommandConfigurationEditor()

    override fun getState(executor: Executor, execEnvironment: ExecutionEnvironment): RunProfileState? {
        val config = clean().ok ?: return null
        return LegacyMoveCommandLineState(execEnvironment, config)
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

    fun clean(): CleanConfiguration {
        val workingDirectory = workingDirectory
            ?: return CleanConfiguration.error("No working directory specified")

        val parsed = ParsedCommand.parse(command)
            ?: return CleanConfiguration.error("No command specified")
        val cmd = MoveCommandLine(
            parsed.command,
            workingDirectory,
            parsed.additionalArguments,
            environmentVariables
        )
        val aptosPath = project.aptosPath
            ?: return CleanConfiguration.error("No Aptos CLI specified")

        if (!aptosPath.exists()) {
            return CleanConfiguration.error("Invalid Aptos CLI: $aptosPath")
        }
        return CleanConfiguration.Ok(cmd, aptosPath)
    }

    sealed class CleanConfiguration {
        class Ok(
            val cmd: MoveCommandLine,
            val aptosLocation: Path,
        ) : CleanConfiguration()

        class Err(val error: RuntimeConfigurationError) : CleanConfiguration()

        val ok: Ok? get() = this as? Ok

        companion object {
            fun error(@NlsContexts.DialogMessage message: String) = Err(
                RuntimeConfigurationError(message)
            )
        }
    }

    data class ParsedCommand(val command: String, val additionalArguments: List<String>) {
        companion object {
            fun parse(rawCommand: String): ParsedCommand? {
                val args = ParametersListUtil.parse(rawCommand)
                val command = args.firstOrNull() ?: return null
                val additionalArguments = args.drop(1)
                return ParsedCommand(command, additionalArguments)
            }
        }
    }
}
