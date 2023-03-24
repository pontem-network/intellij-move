package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import org.move.cli.MoveFileHyperlinkFilter
import java.nio.file.Path

class AptosCommandLineState(
    execEnv: ExecutionEnvironment,
    private val aptosPath: Path,
    private val commandLine: Aptos.CommandLine
) :
    CommandLineState(execEnv) {

    val project = environment.project

    init {
        commandLine.workingDirectory
            ?.let { wd -> addConsoleFilters(MoveFileHyperlinkFilter(project, wd)) }
    }

    override fun startProcess(): ProcessHandler {
        val aptos = Aptos(aptosPath)
        val generalCommandLine = commandLine.toGeneralCommandLine(aptos)

        val handler = KillableColoredProcessHandler(generalCommandLine)
        consoleBuilder.console.attachToProcess(handler)
        ProcessTerminatedListener.attach(handler)  // shows exit code upon termination
        return handler
    }
}
