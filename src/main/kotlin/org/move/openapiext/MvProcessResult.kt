/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.openapiext

import com.fasterxml.jackson.core.JacksonException
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput
import org.move.stdext.RsResult

typealias MvProcessResult<T> = RsResult<T, MvProcessExecutionException>

sealed class MvProcessExecutionOrDeserializationException : RuntimeException {
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
}

class MvDeserializationException(cause: JacksonException) :
    MvProcessExecutionOrDeserializationException(cause)

sealed class MvProcessExecutionException : MvProcessExecutionOrDeserializationException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)

    abstract val commandLineString: String

    class Start(
        override val commandLineString: String,
        cause: ExecutionException,
    ) : MvProcessExecutionException(cause)

    class Canceled(
        override val commandLineString: String,
        val output: ProcessOutput,
        message: String = errorMessage(commandLineString, output),
    ) : MvProcessExecutionException(message)

    class Timeout(
        override val commandLineString: String,
        val output: ProcessOutput,
    ) : MvProcessExecutionException(errorMessage(commandLineString, output))

    /** The process exited with non-zero exit code */
    class ProcessAborted(
        override val commandLineString: String,
        val output: ProcessOutput,
    ) : MvProcessExecutionException(errorMessage(commandLineString, output))

    companion object {
        fun errorMessage(commandLineString: String, output: ProcessOutput): String = """
            |Execution failed (exit code ${output.exitCode}).
            |$commandLineString
            |stdout : ${output.stdout}
            |stderr : ${output.stderr}
        """.trimMargin()
    }
}

fun MvProcessResult<ProcessOutput>.ignoreExitCode(): RsResult<ProcessOutput, MvProcessExecutionException.Start> =
    when (this) {
        is RsResult.Ok -> RsResult.Ok(ok)
        is RsResult.Err -> when (err) {
            is MvProcessExecutionException.Start -> RsResult.Err(err)
            is MvProcessExecutionException.Canceled -> RsResult.Ok(err.output)
            is MvProcessExecutionException.Timeout -> RsResult.Ok(err.output)
            is MvProcessExecutionException.ProcessAborted -> RsResult.Ok(err.output)
        }
    }
