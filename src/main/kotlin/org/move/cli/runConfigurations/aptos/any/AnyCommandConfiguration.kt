package org.move.cli.runConfigurations.aptos.any

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.io.exists
import org.jdom.Element
import org.move.cli.*
import org.move.cli.runConfigurations.aptos.Aptos
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.cli.runConfigurations.aptos.AptosCommandLineState
import org.move.cli.runConfigurations.legacy.MoveCommandConfiguration
import java.nio.file.Path

class AnyCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory
) :
    LocatableConfigurationBase<AptosCommandLineState>(project, factory),
    RunConfigurationWithSuppressedDefaultDebugAction {

    var command: String = "move compile"
    var workingDirectory: Path? = if (!project.isDefault) {
        project.moveProjects.allProjects.firstOrNull()?.contentRootPath
    } else {
        null
    }
    var environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    override fun getConfigurationEditor() = AnyCommandConfigurationEditor()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): AptosCommandLineState? {
        val config = clean().ok ?: return null
        return AptosCommandLineState(environment, config.aptosPath, config.commandLine)
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
        val parsedCommand = MoveCommandConfiguration.ParsedCommand.parse(command)
            ?: return CleanConfiguration.error("No command specified")

        val aptos = Aptos.fromProject(project)
            ?: return CleanConfiguration.error("No Aptos CLI specified")
        if (!aptos.location.exists()) {
            return CleanConfiguration.error("Invalid Aptos CLI: ${aptos.location}")
        }

        val commandLine = AptosCommandLine(
            parsedCommand.command,
            parsedCommand.additionalArguments,
            workingDirectory,
            environmentVariables
        )
        return CleanConfiguration.Ok(aptos.location, commandLine)
    }

    sealed class CleanConfiguration {
        class Ok(val aptosPath: Path, val commandLine: AptosCommandLine) : CleanConfiguration()
        class Err(val error: RuntimeConfigurationError) : CleanConfiguration()

        val ok: Ok? get() = this as? Ok

        companion object {
            fun error(@Suppress("UnstableApiUsage") @NlsContexts.DialogMessage message: String) = Err(
                RuntimeConfigurationError(message)
            )
        }
    }
}
