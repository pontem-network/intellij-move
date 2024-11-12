package org.move.cli.runConfigurations.test

import com.intellij.execution.runners.ExecutionEnvironment
import org.move.cli.runConfigurations.AptosRunStateBase
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.runConfigurations.aptos.AptosTestConsoleBuilder
import org.move.cli.runConfigurations.aptos.cmd.AptosCommandConfiguration

class AptosTestRunState(
    environment: ExecutionEnvironment,
    cleanConfiguration: CommandConfigurationBase.CleanConfiguration.Ok
): AptosRunStateBase(environment, cleanConfiguration) {

    init {
        consoleBuilder =
            AptosTestConsoleBuilder(environment.runProfile as AptosCommandConfiguration, environment.executor)
        createFilters().forEach { consoleBuilder.addFilter(it) }
    }

//    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
//        val processHandler = startProcess()
//        val console = createConsole(executor)
//        console?.attachToProcess(processHandler)
//        return DefaultExecutionResult(console, processHandler).apply { setRestartActions(ToggleAutoTestAction()) }
//    }
}
