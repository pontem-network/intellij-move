package org.move.cli.runConfigurations.test

import com.intellij.execution.runners.ExecutionEnvironment
import org.move.cli.runConfigurations.CliCommandLineArgs
import org.move.cli.runConfigurations.MoveCommandLineState
import java.nio.file.Path

class AptosTestCommandLineState(
    execEnv: ExecutionEnvironment,
    aptosPath: Path,
    commandLine: CliCommandLineArgs
) : MoveCommandLineState(execEnv, aptosPath, commandLine) {

//    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
//        val processHandler = startProcess()
//
//        val consoleProperties = AptosTestConsoleProperties(environment.runProfile as RunConfiguration, executor)
//        val consoleView = SMTestRunnerConnectionUtil.createAndAttachConsole(
//            "Aptos Test", processHandler, consoleProperties
//        ) as SMTRunnerConsoleView
//
//        createFilters().forEach { consoleView.addMessageFilter(it) }
//
//        val executionResult = DefaultExecutionResult(consoleView, processHandler)
//        val rerunFailedTestsAction = consoleProperties.createRerunFailedTestsAction(consoleView)
//        if (rerunFailedTestsAction != null) {
//            rerunFailedTestsAction.setModelProvider(Getter<TestFrameworkRunningModel> { consoleView.resultsViewer })
//            executionResult.setRestartActions(rerunFailedTestsAction, ToggleAutoTestAction())
//        } else {
//            executionResult.setRestartActions(ToggleAutoTestAction())
//        }
//        return executionResult
//    }
}
