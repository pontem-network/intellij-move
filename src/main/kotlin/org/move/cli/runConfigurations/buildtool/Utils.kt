package org.move.cli.runConfigurations.buildtool

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.progress.EmptyProgressIndicator


private val ExecutionEnvironment.executionListener: ExecutionListener
    get() = project.messageBus.syncPublisher(ExecutionManager.EXECUTION_TOPIC)

fun ExecutionEnvironment.notifyProcessStartScheduled() =
    executionListener.processStartScheduled(executor.id, this)

fun ExecutionEnvironment.notifyProcessStarting() =
    executionListener.processStarting(executor.id, this)

fun ExecutionEnvironment.notifyProcessNotStarted() =
    executionListener.processNotStarted(executor.id, this)

fun ExecutionEnvironment.notifyProcessStarted(handler: ProcessHandler) =
    executionListener.processStarted(executor.id, this, handler)

fun ExecutionEnvironment.notifyProcessTerminating(handler: ProcessHandler) =
    executionListener.processTerminating(executor.id, this, handler)

fun ExecutionEnvironment.notifyProcessTerminated(handler: ProcessHandler, exitCode: Int) =
    executionListener.processTerminated(executor.id, this, handler, exitCode)

val ExecutionEnvironment?.isActivateToolWindowBeforeRun: Boolean
    get() = this?.runnerAndConfigurationSettings?.isActivateToolWindowBeforeRun != false

class MockProgressIndicator : EmptyProgressIndicator() {
    private val _textHistory: MutableList<String?> = mutableListOf()
    val textHistory: List<String?> get() = _textHistory

    override fun setText(text: String?) {
        super.setText(text)
        _textHistory += text
    }

    override fun setText2(text: String?) {
        super.setText2(text)
        _textHistory += text
    }
}
