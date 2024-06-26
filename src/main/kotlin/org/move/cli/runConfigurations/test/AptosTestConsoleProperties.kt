package org.move.cli.runConfigurations.test

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator

class AptosTestConsoleProperties(
    runconfig: RunConfiguration,
    executor: Executor
):
    SMTRunnerConsoleProperties(runconfig, TEST_FRAMEWORK_NAME, executor),
    SMCustomMessagesParsing {

    override fun getTestLocator(): SMTestLocator = AptosTestLocator

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: TestConsoleProperties
    ): OutputToGeneralTestEventsConverter =
        AptosTestEventsConverter(testFrameworkName, consoleProperties)

    companion object {
        const val TEST_FRAMEWORK_NAME: String = "Aptos Test"
        const val TEST_TOOL_WINDOW_SETTING_KEY: String = "org.move.aptos.test.tool.window"
    }
}
