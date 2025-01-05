package org.move.cli.runConfigurations.test

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.openapi.util.Key
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor
import org.move.stdext.GsonUtils
import org.move.stdext.partitionLast

private typealias TestNodeId = String

class AptosJsonTestEventsConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties
): OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    override fun processServiceMessages(text: String, outputType: Key<*>, visitor: ServiceMessageVisitor): Boolean {
        val jsonObject = GsonUtils.tryParseJsonObject(text)

        return when {
            jsonObject == null -> false
            handleTestEvent(jsonObject, outputType, visitor) -> true
            handleModuleEvent(jsonObject, outputType, visitor) -> true
            else -> true // don't print unknown json messages
        }
    }

    private fun handleTestEvent(
        jsonObject: JsonObject,
        outputType: Key<*>,
        visitor: ServiceMessageVisitor
    ): Boolean {
        val serviceMessages = AptosTestEvent.fromJson(jsonObject)
            ?.let { createServiceMessagesFor(it) } ?: return false
        for (message in serviceMessages) {
            val message = message.toString()
            super.processServiceMessages(message, outputType, visitor)
        }
        return true
    }

    private fun handleModuleEvent(
        jsonObject: JsonObject,
        outputType: Key<*>,
        visitor: ServiceMessageVisitor
    ): Boolean {
        val moduleEvent = AptosModuleEvent.fromJson(jsonObject) ?: return false
        val messages = createServiceMessagesFor(moduleEvent) ?: return false
        for (message in messages) {
            super.processServiceMessages(message.toString(), outputType, visitor)
        }
        return true
    }

    private fun createServiceMessagesFor(moduleEvent: AptosModuleEvent): List<ServiceMessageBuilder>? {
        val messages = mutableListOf<ServiceMessageBuilder>()
        when (moduleEvent.event) {
            "started" -> {
                processor.onTestsReporterAttached()
                messages.add(createModuleStartedMessage(moduleEvent.module_name))
            }
            "finished" -> {
                messages.add(createModuleFinishedMessage(moduleEvent.module_name))
            }
            else -> return null
        }
        return messages
    }

    private fun createServiceMessagesFor(testEvent: AptosTestEvent): List<ServiceMessageBuilder>? {
        val messages = mutableListOf<ServiceMessageBuilder>()
        when (testEvent.event) {
            "start" -> messages.add(createTestStartedMessage(testEvent.fn_name))
            "pass" -> {
                val duration = parseTestDuration(testEvent)
                messages.add(createTestFinishedMessage(testEvent.fn_name, duration))
            }
            "fail" -> {
                val duration = parseTestDuration(testEvent)
                val failedMessage = testEvent.failure.orEmpty().trim()
                messages.add(createTestFailedMessage(testEvent.fn_name, failedMessage))
                messages.add(createTestFinishedMessage(testEvent.fn_name, duration))
            }
            else -> return null
        }
        return messages
    }

    private fun parseTestDuration(testEvent: AptosTestEvent): String {
        val execTimeText = "${testEvent.exec_time}s"
        return kotlin.time.Duration.parse(execTimeText).inWholeMilliseconds.toString()
    }

    companion object {
        private const val ROOT_SUITE: String = "0"
        private const val NAME_SEPARATOR: String = "::"

        private val TestNodeId.name: String
            get() {
                val (parent, name) = this.partitionLast(NAME_SEPARATOR)
                return when {
                    parent.contains(NAME_SEPARATOR) -> name
                    else -> this
                }
            }

        private val TestNodeId.parent: TestNodeId
            get() {
                val (parent, _) = this.partitionLast(NAME_SEPARATOR)
                return when {
                    parent.contains(NAME_SEPARATOR) -> parent
                    else -> ROOT_SUITE
                }
            }

        private fun createModuleStartedMessage(module: TestNodeId): ServiceMessageBuilder {
            return ServiceMessageBuilder.testSuiteStarted(module.name)
                .addAttribute("nodeId", module)
                .addAttribute("parentNodeId", ROOT_SUITE)
                .addAttribute("locationHint", AptosTestLocator.getTestUrl(module))
        }

        private fun createModuleFinishedMessage(moduleId: TestNodeId): ServiceMessageBuilder =
            ServiceMessageBuilder.testSuiteFinished(moduleId.name)
                .addAttribute("nodeId", moduleId)

        private fun createTestStartedMessage(test: TestNodeId): ServiceMessageBuilder {
            return ServiceMessageBuilder.testStarted(test.name)
                .addAttribute("nodeId", test)
                .addAttribute("parentNodeId", test.parent)
                .addAttribute("locationHint", AptosTestLocator.getTestUrl(test))
        }

        private fun createTestFailedMessage(test: TestNodeId, failedMessage: String): ServiceMessageBuilder {
            val builder = ServiceMessageBuilder.testFailed(test.name)
                .addAttribute("nodeId", test)
                // TODO: pass backtrace here
                .addAttribute("message", failedMessage)
            return builder
        }

        private fun createTestFinishedMessage(test: TestNodeId, duration: String): ServiceMessageBuilder =
            ServiceMessageBuilder.testFinished(test.name)
                .addAttribute("nodeId", test)
                .addAttribute("duration", duration)

//        private fun createTestStdOutMessage(test: TestNodeId, stdout: String): ServiceMessageBuilder =
//            ServiceMessageBuilder.testStdOut(test.name)
//                .addAttribute("nodeId", test)
//                .addAttribute("out", stdout)
    }
}

@Suppress("PropertyName")
private data class AptosModuleEvent(
    val type: String,
    val event: String,
    val module_name: String,
) {
    companion object {
        fun fromJson(json: JsonObject): AptosModuleEvent? {
            if (json.getAsJsonPrimitive("type")?.asString != "module") {
                return null
            }
            return Gson().fromJson(json, AptosModuleEvent::class.java)
        }
    }
}

@Suppress("PropertyName")
private data class AptosTestEvent(
    val type: String,
    val event: String,
    val fn_name: String,
    val failure: String?,
    val exec_time: Float,
) {
    companion object {
        fun fromJson(json: JsonObject): AptosTestEvent? {
            if (json.getAsJsonPrimitive("type")?.asString != "test") {
                return null
            }
            return Gson().fromJson(json, AptosTestEvent::class.java)
        }
    }
}
