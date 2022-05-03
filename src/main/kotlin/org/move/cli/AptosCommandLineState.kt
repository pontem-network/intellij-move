package org.move.cli

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.util.execution.ParametersListUtil
import java.nio.file.Path

class AptosCommandLineState(
    environment: ExecutionEnvironment,
    private val executable: String,
    private val command: String,
    private val workingDirectory: Path?,
    private val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val params =
            ParametersListUtil.parse(this.command).toTypedArray()
        val commandLine =
            GeneralCommandLine(this.executable, *params)
                .withWorkDirectory(this.workingDirectory?.toString().orEmpty())
                .withCharset(Charsets.UTF_8)
        this.environmentVariables.configureCommandLine(commandLine, true)

        val handler = OSProcessHandler(commandLine)
        consoleBuilder.console.attachToProcess(handler)
        ProcessTerminatedListener.attach(handler)  // shows exit code upon termination
        return handler
    }
}
