package org.move.cli.runConfigurations.endless

import com.intellij.execution.process.ProcessOutput
import org.move.openapiext.RsProcessExecutionOrDeserializationException
import org.move.stdext.RsResult

typealias EndlessProcessResult<T> = RsResult<EndlessProcessOutput<T>, RsProcessExecutionOrDeserializationException>

data class EndlessProcessOutput<T>(
    val item: T,
    val output: ProcessOutput,
    val exitStatus: EndlessExitStatus,
) {
    fun <T, U> replaceItem(newItem: U): EndlessProcessOutput<U> = EndlessProcessOutput(newItem, output, exitStatus)
}