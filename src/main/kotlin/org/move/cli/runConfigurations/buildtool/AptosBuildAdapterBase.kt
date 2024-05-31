package org.move.cli.runConfigurations.buildtool

import com.intellij.build.BuildProgressListener
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil.convertLineSeparators

abstract class AptosBuildAdapterBase(
    private val context: AptosBuildContextBase,
    protected val buildProgressListener: BuildProgressListener
) : ProcessAdapter() {
    private val instantReader = BuildOutputInstantReaderImpl(
        context.buildId,
        context.parentId,
        buildProgressListener,
        listOf(AptosBuildEventsConverter(context))
    )

    override fun processTerminated(event: ProcessEvent) {
        instantReader.closeAndGetFuture().whenComplete { _, error ->
            val isSuccess = event.exitCode == 0 && context.errors.get() == 0
            val isCanceled = context.indicator?.isCanceled ?: false
            onBuildOutputReaderFinish(event, isSuccess = isSuccess, isCanceled = isCanceled, error)
        }
    }

    open fun onBuildOutputReaderFinish(event: ProcessEvent, isSuccess: Boolean, isCanceled: Boolean, error: Throwable?) {}

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        // Progress messages end with '\r' instead of '\n'. We want to replace '\r' with '\n'
        // so that `instantReader` sends progress messages to parsers separately from other messages.
        val text = convertLineSeparators(event.text)
        instantReader.append(text)
    }
}
