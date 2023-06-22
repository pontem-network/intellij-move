package org.move.cli.runConfigurations.test

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator

class AptosTestConsoleProperties(runconfig: RunConfiguration, executor: Executor) :
    SMTRunnerConsoleProperties(runconfig, "Aptos Test", executor),
    SMCustomMessagesParsing {

    override fun getTestLocator(): SMTestLocator = AptosTestLocator

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: TestConsoleProperties
    ): OutputToGeneralTestEventsConverter =
        AptosTestEventsConverter(consoleProperties, testFrameworkName)
}
