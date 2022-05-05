/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.openapiext

import com.fasterxml.jackson.core.JacksonException
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput
import org.move.stdext.MvResult

typealias MvProcessResult<T> = MvResult<T, MvProcessExecutionException>

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

fun MvProcessResult<ProcessOutput>.ignoreExitCode(): MvResult<ProcessOutput, MvProcessExecutionException.Start> =
    when (this) {
        is MvResult.Ok -> MvResult.Ok(ok)
        is MvResult.Err -> when (err) {
            is MvProcessExecutionException.Start -> MvResult.Err(err)
            is MvProcessExecutionException.Canceled -> MvResult.Ok(err.output)
            is MvProcessExecutionException.Timeout -> MvResult.Ok(err.output)
            is MvProcessExecutionException.ProcessAborted -> MvResult.Ok(err.output)
        }
    }
