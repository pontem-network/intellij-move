package org.move.cli

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.move.cli.runconfig.AptosCommandConfiguration

class AptosCommandLineState(
    environment: ExecutionEnvironment,
    val runConfiguration: AptosCommandConfiguration,
    val config: AptosCommandConfiguration.CleanConfiguration.Ok,
) : CommandLineState(environment) {

    val project: Project = environment.project
    val commandLine: AptosCommandLine = config.cmd

    init {
        commandLine.workingDirectory
            ?.let { wd -> addConsoleFilters(MoveFileHyperlinkFilter(project, wd)) }
    }

    override fun startProcess(): ProcessHandler {
        val generalCommandLine =
            this.config.aptos.toGeneralCommandLine(project, commandLine)

//        val params =
//            ParametersListUtil.parse(commandLine.command).toTypedArray()
//        val generalCommandLine =
//            GeneralCommandLine(
//                this.config.aptos.locationString,
//                *params
//            )
//                .withWorkDirectory(commandLine.workingDirectory?.toString()?.orEmpty())
//                .withCharset(Charsets.UTF_8)
//        this.environmentVariables.configureCommandLine(generalCommandLine, true)

        val handler = KillableColoredProcessHandler(generalCommandLine)
        consoleBuilder.console.attachToProcess(handler)
        ProcessTerminatedListener.attach(handler)  // shows exit code upon termination
        return handler
    }
}
