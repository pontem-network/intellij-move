package org.move.cli.runConfigurations.test

import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.openapi.util.Key
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor

//data class LibtestSuiteMessage(
//    val type: String,
//    val event: String,
//    val test_count: String
//) {
//    companion object {
//        fun fromJson(json: JsonObject): LibtestSuiteMessage? {
//            if (json.getAsJsonPrimitive("type")?.asString != "suite") {
//                return null
//            }
//
//            return Gson().fromJson(json, LibtestSuiteMessage::class.java)
//        }
//    }
//}

//data class LibtestTestMessage(
//    val type: String,
//    val event: String,
//    val name: String,
//    val stdout: String
//) {
//    companion object {
//        fun fromJson(json: JsonObject): LibtestTestMessage? {
//            if (json.getAsJsonPrimitive("type")?.asString != "test") {
//                return null
//            }
//
//            return Gson().fromJson(json, LibtestTestMessage::class.java)
//        }
//    }
//}

sealed class TestLine {
    object StartTests : TestLine()
    object EndTests : TestLine()

    data class StartModuleFailDetail(val moduleName: String) : TestLine()

    data class Pass(val moduleName: String, val testName: String) : TestLine()
    data class Fail(val moduleName: String, val testName: String) : TestLine()

    data class StartTestFailDetail(val moduleName: String, val testName: String) : TestLine()
    data class EndTestFailDetail(val moduleName: String, val testName: String) : TestLine()

    companion object {
        fun parse(textLine: String): TestLine? {
            val trimmedLine = textLine.trim()
            return when {
                trimmedLine == "Running Move unit tests" -> StartTests
                trimmedLine == "Test result:" -> EndTests

//                trimmedLine.startsWith("┌──") -> {
//                    val testName = textLine.split(" ")[1]
//                    StartTestFailDetail(testName)
//                }
//                trimmedLine == "└──────────────────" -> EndTestFailDetail

//                textLine.startsWith("[ PASS    ]") -> {
//                    val testFqName = textLine.split(" ").lastOrNull() ?: return null
//                    Pass(testFqName)
//                }
//                textLine.startsWith("[ FAIL    ]") -> {
//                    val testFqName = textLine.split(" ").lastOrNull() ?: return null
//                    Fail(testFqName)
//                }

                else -> null
            }
        }
    }
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

class AptosTestEventsConverter(
    consoleProperties: TestConsoleProperties,
    testFrameworkName: String
) : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    private val parser = TestLineParser()

    override fun processServiceMessages(
        textLine: String,
        outputType: Key<*>,
        visitor: ServiceMessageVisitor
    ): Boolean {
        val testLine = parser.parse(textLine) ?: return false
        val messages = createServiceMessagesFor(testLine) ?: return false
        if (testLine is TestLine.StartTests) {
            super.processServiceMessages(
                ServiceMessageBuilder.testsStarted().toString(),
                outputType,
                visitor
            )
            return true
        }
        messages
            .map { it.toString() }
            .forEach { super.processServiceMessages(it, outputType, visitor) }
        return false
    }

    private fun createServiceMessagesFor(testLine: TestLine): List<ServiceMessageBuilder>? {
        return when (testLine) {
            is TestLine.Pass -> {
                val testId = "${testLine.moduleName}::${testLine.testName}"
                listOf(
                    ServiceMessageBuilder.testStarted(testId),
                    ServiceMessageBuilder.testFinished(testId)
                )
            }
            is TestLine.Fail -> {
                val testId = "${testLine.moduleName}::${testLine.testName}"
                listOf(
                    ServiceMessageBuilder.testStarted(testId),
                    ServiceMessageBuilder.testFailed(testId)
                        .addAttribute("message", ""),
                    ServiceMessageBuilder.testFinished(testId)
                )
            }
            is TestLine.StartTestFailDetail -> {
                val testId = "${testLine.moduleName}::${testLine.testName}"
                listOf(ServiceMessageBuilder.testStarted(testLine.testName))
            }
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
}
