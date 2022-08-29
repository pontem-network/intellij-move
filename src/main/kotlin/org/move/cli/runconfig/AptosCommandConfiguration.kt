package org.move.cli.runconfig

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.execution.ParametersListUtil
import org.jdom.Element
import org.move.cli.*
import java.nio.file.Path

class AptosCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : LocatableConfigurationBase<RunProfileState>(project, factory, "Move"),
    RunConfigurationWithSuppressedDefaultDebugAction {

    var command: String = "move compile"
    var workingDirectory: Path? = if (!project.isDefault) {
        project.moveProjects.allProjects.firstOrNull()?.contentRootPath
    } else {
        null
    }
    var environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        val config = clean()
        if (config is CleanConfiguration.Err) throw config.error
    }

    override fun getConfigurationEditor() = MoveRunConfigurationEditor()

    override fun getState(executor: Executor, execEnvironment: ExecutionEnvironment): RunProfileState? {
        val config = clean().ok ?: return null
        return AptosCommandLineState(execEnvironment, this, config)
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

        val cmd = run {
            val parsed = ParsedCommand.parse(command)
                ?: return CleanConfiguration.error("No command specified")
            AptosCommandLine(
                parsed.command,
                workingDirectory,
                parsed.additionalArguments,
                environmentVariables
            )
        }

        val aptos = project.aptos
            ?: return CleanConfiguration.error("No Aptos CLI specified")

        if (!aptos.isValidLocation()) {
            return CleanConfiguration.error("Invalid Aptos CLI: ${aptos.location}")
        }
        return CleanConfiguration.Ok(cmd, aptos)
    }

    sealed class CleanConfiguration {
        class Ok(
            val cmd: AptosCommandLine,
            val aptos: Aptos,
        ) : CleanConfiguration()

        class Err(val error: RuntimeConfigurationError) : CleanConfiguration()

        val ok: Ok? get() = this as? Ok

        companion object {
            fun error(@Suppress("UnstableApiUsage") @NlsContexts.DialogMessage message: String) = Err(
                RuntimeConfigurationError(message)
            )
        }
    }
}

data class ParsedCommand(val command: String, val additionalArguments: List<String>) {
    companion object {
        fun parse(rawCommand: String): ParsedCommand? {
            val args = ParametersListUtil.parse(rawCommand)
            val command = args.firstOrNull { !it.startsWith("+") } ?: return null
            val additionalArguments = args.drop(args.indexOf(command) + 1)
            return ParsedCommand(command, additionalArguments)
        }
    }
}
