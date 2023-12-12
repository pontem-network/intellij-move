package org.move.cli.runConfigurations.aptos

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import org.jdom.Element
import org.move.cli.readPath
import org.move.cli.readString
import org.move.cli.runConfigurations.legacy.MoveCommandConfiguration
import org.move.cli.writePath
import org.move.cli.writeString
import org.move.stdext.exists
import java.nio.file.Path

abstract class CommandConfigurationBase(
    project: Project,
    factory: ConfigurationFactory
) :
    LocatableConfigurationBase<AptosCommandLineState>(project, factory),
    RunConfigurationWithSuppressedDefaultDebugAction {

    var command: String = ""
    var workingDirectory: Path? = null
    var environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

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

    override fun getState(executor: Executor, environment: ExecutionEnvironment): AptosCommandLineState? {
        return clean().ok?.let { stateConfig ->
//            AptosCommandLineState(environment, stateConfig.aptosPath, stateConfig.commandLine)
//            if (command.startsWith("move test")) {
//                AptosTestCommandLineState(environment, stateConfig.aptosPath, stateConfig.commandLine)
//            } else {
            AptosCommandLineState(environment, stateConfig.aptosPath, stateConfig.commandLine)
//            }
        }
    }

    fun clean(): CleanConfiguration {
        val workingDirectory = workingDirectory
            ?: return CleanConfiguration.error("No working directory specified")
        val parsedCommand = MoveCommandConfiguration.ParsedCommand.parse(command)
            ?: return CleanConfiguration.error("No command specified")

        val aptosCli = AptosCliExecutor.fromProject(project)
            ?: return CleanConfiguration.error("No Aptos CLI specified")
        if (!aptosCli.location.exists()) {
            return CleanConfiguration.error("Invalid Aptos CLI: ${aptosCli.location}")
        }

        val commandLine = AptosCommandLine(
            parsedCommand.command,
            parsedCommand.additionalArguments,
            workingDirectory,
            environmentVariables
        )
        return CleanConfiguration.Ok(aptosCli.location, commandLine)
    }

    sealed class CleanConfiguration {
        class Ok(val aptosPath: Path, val commandLine: AptosCommandLine) : CleanConfiguration()
        class Err(val error: RuntimeConfigurationError) : CleanConfiguration()

        val ok: Ok? get() = this as? Ok

        companion object {
            fun error(@NlsContexts.DialogMessage message: String) = Err(
                RuntimeConfigurationError(message)
            )
        }
    }
}
