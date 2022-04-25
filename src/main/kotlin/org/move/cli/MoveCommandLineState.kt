package org.move.cli

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.util.execution.ParametersListUtil
import org.move.cli.runconfig.MoveCmd

class MoveCommandLineState(
    environment: ExecutionEnvironment,
    private val executable: String,
    private val cmd: MoveCmd
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val params =
            ParametersListUtil.parse(this.cmd.command).toTypedArray()
        val commandLine =
            GeneralCommandLine(this.executable, *params)
                .withWorkDirectory(this.cmd.workingDirectory?.toString().orEmpty())
                .withCharset(Charsets.UTF_8)
        this.cmd.environmentVariables.configureCommandLine(commandLine, true)

        val handler = OSProcessHandler(commandLine)
        consoleBuilder.console.attachToProcess(handler)
        ProcessTerminatedListener.attach(handler)  // shows exit code upon termination
        return handler
    }
}
