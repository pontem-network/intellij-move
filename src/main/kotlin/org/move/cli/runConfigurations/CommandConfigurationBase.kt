package org.move.cli.runConfigurations

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.execution.ParametersListUtil
import org.jdom.Element
import org.move.cli.readPath
import org.move.cli.readString
import org.move.cli.runConfigurations.CommandConfigurationBase.CleanConfiguration.Companion.configurationError
import org.move.cli.runConfigurations.test.AptosTestConsoleProperties.Companion.TEST_TOOL_WINDOW_SETTING_KEY
import org.move.cli.runConfigurations.test.AptosTestRunState
import org.move.cli.settings.aptosCliPath
import org.move.cli.writePath
import org.move.cli.writeString
import org.move.openapiext.rootPluginDisposable
import org.move.stdext.exists
import java.nio.file.Path

abstract class CommandConfigurationBase(
    project: Project,
    factory: ConfigurationFactory
):
    LocatableConfigurationBase<AptosRunState>(project, factory),
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

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        val config = clean()
        if (config is CleanConfiguration.Err) {
            throw config.error
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): AptosRunStateBase? {
        val config = clean().ok ?: return null

        // environment is disposable to which all internal RunState disposables are connected
        // todo: find shorter living disposable?
        Disposer.register(project.rootPluginDisposable, environment)

        return if (showTestToolWindow(config.cmd)) {
            AptosTestRunState(environment, config)
        } else {
            AptosRunState(environment, config)
        }
    }

    fun clean(): CleanConfiguration {
        val (subcommand, arguments) = parseAptosCommand(command)
            ?: return configurationError("No subcommand specified")
        val workingDirectory = workingDirectory
            ?: return configurationError("No working directory specified")

        val aptosPath = project.aptosCliPath ?: return configurationError("No Aptos CLI specified")
        if (!aptosPath.exists()) {
            return configurationError("Invalid Aptos CLI location: $aptosPath")
        }
        val commandLine =
            AptosCommandLine(
                subcommand,
                arguments,
                workingDirectory,
                environmentVariables
            )
        return CleanConfiguration.
        Ok(aptosPath, commandLine)
    }

    protected fun showTestToolWindow(commandLine: AptosCommandLine): Boolean {
        return when {
            !AdvancedSettings.getBoolean(TEST_TOOL_WINDOW_SETTING_KEY) -> false
            commandLine.subCommand != "move test" -> false
            else -> true
        }
    }

    sealed class CleanConfiguration {
        class Ok(val aptosPath: Path, val cmd: AptosCommandLine): CleanConfiguration()
        class Err(val error: RuntimeConfigurationError): CleanConfiguration()

        val ok: Ok? get() = this as? Ok

        companion object {
            fun configurationError(@NlsContexts.DialogMessage message: String) = Err(
                RuntimeConfigurationError(message)
            )
        }
    }

    companion object {
        fun parseAptosCommand(rawAptosCommand: String): Pair<String, List<String>>? {
            val args = ParametersListUtil.parse(rawAptosCommand)
            val rootCommand = args.firstOrNull() ?: return null
            return if (rootCommand == "move") {
                val subcommand = args.drop(1).firstOrNull() ?: return null
                val command = "move $subcommand"
                val additionalArguments = args.drop(2)
                Pair(command, additionalArguments)
            } else {
                val additionalArguments = args.drop(1)
                Pair(rootCommand, additionalArguments)
            }
        }
    }
}
