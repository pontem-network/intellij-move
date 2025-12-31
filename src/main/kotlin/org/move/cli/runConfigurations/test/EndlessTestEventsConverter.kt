package org.move.cli.runConfigurations.test

import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.openapi.util.Key
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor

private typealias NodeId = String

sealed class TestLine {
    object StartTests: TestLine()
    object EndTests: TestLine()

    data class StartModuleFailDetail(val moduleName: String): TestLine()

    data class Pass(val moduleName: String, val testName: String): TestLine()
    data class Fail(val moduleName: String, val testName: String): TestLine()

    data class StartTestFailDetail(val moduleName: String, val testName: String): TestLine()
    data class EndTestFailDetail(val moduleName: String, val testName: String): TestLine()
}

class TestLineParser {
    private var currentModuleName: String? = null
    private var currentTestName: String? = null

    fun parse(textLine: String): TestLine? {
        val trimmedLine = textLine.trim()
        return when {
            trimmedLine == "Running Move unit tests" -> TestLine.StartTests
            trimmedLine == "Test result:" -> TestLine.EndTests

            trimmedLine.startsWith("Failures in") -> {
                val moduleName = trimmedLine.split(" in ").last().removeSuffix(":")
                currentModuleName = moduleName
                return TestLine.StartModuleFailDetail(moduleName)
            }

            trimmedLine.startsWith("┌──") -> {
                val moduleName = currentModuleName ?: return null
                val testName = textLine.split(" ").getOrNull(1) ?: return null
                currentTestName = testName
                TestLine.StartTestFailDetail(moduleName, testName)
            }
            trimmedLine == "└──────────────────" -> {
                val moduleName = currentModuleName ?: return null
                val testName = currentTestName ?: return null
                currentTestName = null
                TestLine.EndTestFailDetail(moduleName, testName)
            }

            textLine.startsWith("[ PASS    ]") -> {
                val testFqName = textLine.split(" ").lastOrNull() ?: return null

                val parts = testFqName.split("::")
                val address = parts.getOrNull(0) ?: return null
                val moduleName = parts.getOrNull(1) ?: return null
                val testName = parts.getOrNull(2) ?: return null

                TestLine.Pass("$address::$moduleName", testName)
            }
            textLine.startsWith("[ FAIL    ]") -> {
                val testFqName = textLine.split(" ").lastOrNull() ?: return null

                val parts = testFqName.split("::")
                val address = parts.getOrNull(0) ?: return null
                val moduleName = parts.getOrNull(1) ?: return null
                val testName = parts.getOrNull(2) ?: return null

                TestLine.Fail("$address::$moduleName", testName)
            }

            else -> null
        }
    }

}

class EndlessTestEventsConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties
): OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    private val linesParser = TestLineParser()

    override fun processServiceMessages(
        textLine: String,
        outputType: Key<*>,
        visitor: ServiceMessageVisitor
    ): Boolean {
        val testLine = linesParser.parse(textLine) ?: return false
        val messages = createServiceMessagesFor(testLine) ?: return false
        if (testLine is TestLine.StartTests) {
            super.processServiceMessages(
                ServiceMessageBuilder.testsStarted().toString(),
                outputType,
                visitor
            )
            return true
        }
        for (message in messages) {
            super.processServiceMessages(message.toString(), outputType, visitor)
        }
        return false
    }

    private fun createServiceMessagesFor(testLine: TestLine): List<ServiceMessageBuilder>? {
        return when (testLine) {
            is TestLine.Pass -> {
                val test = "${testLine.moduleName}::${testLine.testName}"
                listOf(
                    createTestStartedMessage(test),
                    createTestFinishedMessage(test)
                )
            }
            is TestLine.Fail -> {
                val test = "${testLine.moduleName}::${testLine.testName}"
                listOf(
                    createTestStartedMessage(test),
                    createTestFailedMessage(test),
                    createTestFinishedMessage(test)
                )
            }
//            is TestLine.StartTestFailDetail -> {
//                val testId = "${testLine.moduleName}::${testLine.testName}"
//                listOf(ServiceMessageBuilder.testStarted(testLine.testName))
//            }
//            is TestLine.EndTestFailDetail -> {
//                val lastTestName = lastFailedTest ?: return null
//                lastFailedTest = null
//                listOf(
//                    ServiceMessageBuilder.testFailed(lastTestName)
//                        .addAttribute("message", ""),
//                    ServiceMessageBuilder.testFinished(lastTestName)
//                )
//            }
            else -> null
        }
    }

    companion object {
        private const val ROOT_SUITE: String = "0"
        private const val NAME_SEPARATOR: String = "::"

//        private val NodeId.name: String
//            get() = if (contains(NAME_SEPARATOR)) {
//                // target_name-xxxxxxxxxxxxxxxx::name1::name2 -> name2
//                substringAfterLast(NAME_SEPARATOR)
//            } else {
//                // target_name-xxxxxxxxxxxxxxxx -> target_name
//                val targetName = substringBeforeLast("-")
//                // Add a tag to distinguish a doc-test suite from a lib suite
//                if (endsWith(DOCTESTS_SUFFIX)) "$targetName (doc-tests)" else targetName
//            }

        private val NodeId.parent: NodeId
            get() {
                val parent = substringBeforeLast(NAME_SEPARATOR)
                return if (this == parent) ROOT_SUITE else parent
            }

        private fun createTestSuiteStartedMessage(suite: NodeId): ServiceMessageBuilder =
            ServiceMessageBuilder.testSuiteStarted(suite)
                .addAttribute("nodeId", suite)
                .addAttribute("parentNodeId", suite.parent)
//                .addAttribute("locationHint", CargoTestLocator.getTestUrl(suite))

        private fun createTestSuiteFinishedMessage(suite: NodeId): ServiceMessageBuilder =
            ServiceMessageBuilder.testSuiteFinished(suite)
                .addAttribute("nodeId", suite)

        private fun createTestStartedMessage(test: NodeId): ServiceMessageBuilder {
            val builder = ServiceMessageBuilder.testStarted(test)
                .addAttribute("nodeId", test)
                .addAttribute("parentNodeId", test.parent)
                .addAttribute("locationHint", EndlessTestLocator.getTestUrl(test))
            return builder
        }

        private fun createTestFinishedMessage(testId: NodeId): ServiceMessageBuilder {
            val builder = ServiceMessageBuilder.testFinished(testId)
                .addAttribute("nodeId", testId)
            return builder
        }

        private fun createTestFailedMessage(testId: NodeId): ServiceMessageBuilder {
            val builder = ServiceMessageBuilder.testFailed(testId)
                .addAttribute("nodeId", testId)
                .addAttribute("message", "")
            return builder
        }
    }
}
