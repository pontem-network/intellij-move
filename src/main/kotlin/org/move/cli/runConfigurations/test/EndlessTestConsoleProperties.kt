package org.move.cli.runConfigurations.test

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator

class EndlessTestConsoleProperties(
    runconfig: RunConfiguration,
    executor: Executor
):
    SMTRunnerConsoleProperties(runconfig, TEST_FRAMEWORK_NAME, executor),
    SMCustomMessagesParsing {

    init {
        isIdBasedTestTree = false
    }

    override fun getTestLocator(): SMTestLocator = EndlessTestLocator

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: TestConsoleProperties
    ): OutputToGeneralTestEventsConverter =
        EndlessTestEventsConverter(testFrameworkName, consoleProperties)

    companion object {
        const val TEST_FRAMEWORK_NAME: String = "Endless Test"
        const val TEST_TOOL_WINDOW_SETTING_KEY: String = "org.move.endless.test.tool.window"
    }
}
