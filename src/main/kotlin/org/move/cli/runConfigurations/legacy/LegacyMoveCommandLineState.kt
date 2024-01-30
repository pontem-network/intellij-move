package org.move.cli.runConfigurations.legacy

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.move.cli.MoveFileHyperlinkFilter
import org.move.openapiext.withWorkDirectory
import java.nio.file.Path

class LegacyMoveCommandLineState(
    environment: ExecutionEnvironment,
    val config: MoveCommandConfiguration.CleanConfiguration.Ok,
) : CommandLineState(environment) {

    val project: Project = environment.project
    val commandLine: MoveCommandLine = config.cmd

    init {
        commandLine.workingDirectory
            ?.let { wd -> addConsoleFilters(MoveFileHyperlinkFilter(project, wd)) }
    }

    override fun startProcess(): ProcessHandler {
        val generalCommandLine =
            toGeneralCommandLine(this.config.aptosLocation, commandLine)
        val handler = KillableColoredProcessHandler(generalCommandLine)
        consoleBuilder.console.attachToProcess(handler)
        ProcessTerminatedListener.attach(handler)  // shows exit code upon termination
        return handler
    }
}

private fun toGeneralCommandLine(aptosLocation: Path, commandLine: MoveCommandLine): GeneralCommandLine {
    val generalCommandLine =
        GeneralCommandLine(
            aptosLocation.toString(),
            commandLine.command,
            *commandLine.additionalArguments.toTypedArray()
        )
            .withWorkDirectory(commandLine.workingDirectory)
            .withCharset(Charsets.UTF_8)
    commandLine.environmentVariables.configureCommandLine(generalCommandLine, true)
    return generalCommandLine
}
