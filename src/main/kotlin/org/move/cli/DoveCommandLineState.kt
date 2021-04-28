package org.move.cli

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.util.execution.ParametersListUtil
import org.move.settings.dovePathValue

class DoveCommandLineState(
    environment: ExecutionEnvironment,
    private val runConfiguration: DoveRunConfiguration,
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val pathToExecutable = runConfiguration.project.dovePathValue
        val params = ParametersListUtil.parse(runConfiguration.command).toTypedArray()
        val commandLine =
            GeneralCommandLine(pathToExecutable, *params)
                .withWorkDirectory(runConfiguration.workingDirectory.toString())
                .withCharset(Charsets.UTF_8)

        val handler = OSProcessHandler(commandLine)
        consoleBuilder.console.attachToProcess(handler)
        ProcessTerminatedListener.attach(handler)  // shows exit code upon termination
        return handler
    }
}
