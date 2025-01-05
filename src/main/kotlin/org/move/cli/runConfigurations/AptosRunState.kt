package org.move.cli.runConfigurations

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.filters.Filter
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Disposer
import org.move.cli.MoveFileHyperlinkFilter
import org.move.cli.runConfigurations.CommandConfigurationBase.CleanConfiguration
import org.move.cli.runConfigurations.buildtool.AptosPatch
import org.move.cli.runConfigurations.buildtool.aptosPatches

abstract class AptosRunStateBase(
    environment: ExecutionEnvironment,
    val cleanConfiguration: CleanConfiguration.Ok
): CommandLineState(environment) {

    val project = environment.project
    val commandLine: AptosCommandLine = cleanConfiguration.cmd

    protected val commandLinePatches: MutableList<AptosPatch> = mutableListOf()

    init {
        commandLinePatches.addAll(environment.aptosPatches)
    }

    fun prepareCommandLine(vararg additionalPatches: AptosPatch): AptosCommandLine {
        var commandLine = commandLine
        for (patch in commandLinePatches) {
            commandLine = patch(commandLine)
        }
        for (patch in additionalPatches) {
            commandLine = patch(commandLine)
        }
        return commandLine
    }

    override fun startProcess(): ProcessHandler {
        val commandLine = prepareCommandLine()
        // emulateTerminal=true allows for the colored output
        val generalCommandLine =
            commandLine.toGeneralCommandLine(cleanConfiguration.aptosPath, emulateTerminal = true)
        val handler = KillableColoredProcessHandler(generalCommandLine)

        val console = consoleBuilder.console
        Disposer.register(this.environment, console)
        console.attachToProcess(handler)

        ProcessTerminatedListener.attach(handler)  // shows exit code upon termination
        return handler
    }

    protected fun createFilters(): Collection<Filter> {
        val filters = mutableListOf<Filter>()
        val wd = commandLine.workingDirectory
        if (wd != null) {
            filters.add(MoveFileHyperlinkFilter(project, wd))
        }
        return filters
    }
}

class AptosRunState(
    environment: ExecutionEnvironment, config: CleanConfiguration.Ok
): AptosRunStateBase(environment, config) {

    init {
        createFilters().forEach { consoleBuilder.addFilter(it) }
    }

}
