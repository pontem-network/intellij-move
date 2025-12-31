package org.move.cli.runConfigurations.test

import com.intellij.execution.runners.ExecutionEnvironment
import org.move.cli.runConfigurations.EndlessRunStateBase
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.runConfigurations.endless.EndlessTestConsoleBuilder
import org.move.cli.runConfigurations.endless.cmd.EndlessCommandConfiguration

class EndlessTestRunState(
    environment: ExecutionEnvironment,
    cleanConfiguration: CommandConfigurationBase.CleanConfiguration.Ok
): EndlessRunStateBase(environment, cleanConfiguration) {

    init {
        consoleBuilder =
            EndlessTestConsoleBuilder(environment.runProfile as EndlessCommandConfiguration, environment.executor)
        createFilters().forEach { consoleBuilder.addFilter(it) }
    }

//    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
//        val processHandler = startProcess()
//        val console = createConsole(executor)
//        console?.attachToProcess(processHandler)
//        return DefaultExecutionResult(console, processHandler).apply { setRestartActions(ToggleAutoTestAction()) }
//    }
}
