package org.move.cli.runConfigurations.test

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.autotest.ToggleAutoTestAction
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import org.move.cli.runConfigurations.AptosRunStateBase
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.runConfigurations.aptos.AptosTestConsoleBuilder
import org.move.cli.runConfigurations.aptos.cmd.AptosCommandConfiguration

class AptosTestRunState(
    environment: ExecutionEnvironment,
    runConfiguration: CommandConfigurationBase,
    config: CommandConfigurationBase.CleanConfiguration.Ok
): AptosRunStateBase(environment, runConfiguration, config) {

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
