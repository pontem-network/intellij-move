package org.move.cli.runConfigurations.test

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.TestFrameworkRunningModel
import com.intellij.execution.testframework.autotest.ToggleAutoTestAction
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.openapi.util.Getter
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.cli.runConfigurations.aptos.AptosCommandLineState
import java.nio.file.Path

class AptosTestCommandLineState(
    execEnv: ExecutionEnvironment,
    aptosPath: Path,
    commandLine: AptosCommandLine
) : AptosCommandLineState(execEnv, aptosPath, commandLine) {

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler = startProcess()

        val consoleProperties = AptosTestConsoleProperties(environment.runProfile as RunConfiguration, executor)
        val consoleView = SMTestRunnerConnectionUtil.createAndAttachConsole(
            "Aptos Test", processHandler, consoleProperties
        ) as SMTRunnerConsoleView

        createFilters().forEach { consoleView.addMessageFilter(it) }

        val executionResult = DefaultExecutionResult(consoleView, processHandler)
        val rerunFailedTestsAction = consoleProperties.createRerunFailedTestsAction(consoleView)
        if (rerunFailedTestsAction != null) {
            rerunFailedTestsAction.setModelProvider(Getter<TestFrameworkRunningModel> { consoleView.resultsViewer })
            executionResult.setRestartActions(rerunFailedTestsAction, ToggleAutoTestAction())
        } else {
            executionResult.setRestartActions(ToggleAutoTestAction())
        }
        return executionResult
    }
}
