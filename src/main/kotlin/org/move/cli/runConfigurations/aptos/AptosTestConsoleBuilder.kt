package org.move.cli.runConfigurations.aptos

import com.intellij.execution.Executor
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.ui.ConsoleView
import org.move.cli.runConfigurations.aptos.cmd.AptosCommandConfiguration
import org.move.cli.runConfigurations.test.AptosTestConsoleProperties.Companion.TEST_FRAMEWORK_NAME

class AptosTestConsoleBuilder(
    private val config: AptosCommandConfiguration,
    private val executor: Executor
): TextConsoleBuilder() {

    private val filters: MutableList<Filter> = mutableListOf()

    override fun addFilter(filter: Filter) {
        filters.add(filter)
    }

    override fun setViewer(isViewer: Boolean) {}

    override fun getConsole(): ConsoleView {
        val consoleProperties = config.createTestConsoleProperties(executor)
        val consoleView = SMTestRunnerConnectionUtil.createConsole(TEST_FRAMEWORK_NAME, consoleProperties!!)
        filters.forEach { consoleView.addMessageFilter(it) }
        return consoleView

    }
}