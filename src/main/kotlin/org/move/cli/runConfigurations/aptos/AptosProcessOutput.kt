package org.move.cli.runConfigurations.aptos

import com.intellij.execution.process.ProcessOutput
import org.move.openapiext.RsProcessExecutionOrDeserializationException
import org.move.stdext.RsResult

typealias AptosProcessResult<T> = RsResult<AptosProcessOutput<T>, RsProcessExecutionOrDeserializationException>

data class AptosProcessOutput<T>(
    val item: T,
    val output: ProcessOutput,
    val exitStatus: AptosExitStatus,
) {
    fun <T, U> replaceItem(newItem: U): AptosProcessOutput<U> = AptosProcessOutput(newItem, output, exitStatus)
}