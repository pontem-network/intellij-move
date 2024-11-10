/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.openapiext

import com.fasterxml.jackson.core.JacksonException
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput
import org.move.stdext.RsResult

typealias RsProcessResult<T> = RsResult<T, RsProcessExecutionException>

sealed class RsProcessExecutionOrDeserializationException : RuntimeException {
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
}

class RsDeserializationException(cause: JacksonException) :
    RsProcessExecutionOrDeserializationException(cause)

sealed class RsProcessExecutionException : RsProcessExecutionOrDeserializationException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)

    abstract val commandLineString: String

    class Start(
        override val commandLineString: String,
        cause: ExecutionException,
    ) : RsProcessExecutionException(cause)

    class Canceled(
        override val commandLineString: String,
        val output: ProcessOutput,
        message: String = errorMessage(commandLineString, output),
    ) : RsProcessExecutionException(message)

    class Timeout(
        override val commandLineString: String,
        val output: ProcessOutput,
    ) : RsProcessExecutionException(errorMessage(commandLineString, output))

    /** The process exited with non-zero exit code */
    class FailedWithNonZeroExitCode(
        override val commandLineString: String,
        val output: ProcessOutput,
    ) : RsProcessExecutionException(errorMessage(commandLineString, output))

    companion object {
        fun errorMessage(commandLineString: String, output: ProcessOutput): String = """
            |Execution failed (exit code ${output.exitCode}).
            |$commandLineString
            |stdout : ${output.stdout}
            |stderr : ${output.stderr}
        """.trimMargin()
    }
}

fun RsProcessResult<ProcessOutput>.ignoreExitCode(): RsResult<ProcessOutput, RsProcessExecutionException.Start> =
    when (this) {
        is RsResult.Ok -> RsResult.Ok(ok)
        is RsResult.Err -> when (err) {
            is RsProcessExecutionException.Start -> RsResult.Err(err)
            is RsProcessExecutionException.Canceled -> RsResult.Ok(err.output)
            is RsProcessExecutionException.Timeout -> RsResult.Ok(err.output)
            is RsProcessExecutionException.FailedWithNonZeroExitCode -> RsResult.Ok(err.output)
        }
    }
