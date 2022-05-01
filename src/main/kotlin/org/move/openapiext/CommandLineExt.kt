package org.move.openapiext

/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.io.systemIndependentPath
import org.move.cli.runconfig.MvCapturingProcessHandler
import org.move.stdext.unwrapOrElse
import java.nio.file.Path

private val LOG = Logger.getInstance("org.rust.openapiext.CommandLineExt")

//@Suppress("FunctionName")
//fun GeneralCommandLine(path: Path, vararg args: String) =
//    GeneralCommandLine(path.systemIndependentPath, *args)

fun GeneralCommandLine.withWorkDirectory(path: Path?) = withWorkDirectory(path?.systemIndependentPath)

//fun GeneralCommandLine.execute(timeoutInMilliseconds: Int? = 1000): ProcessOutput? {
//    val output = try {
//        val handler = CapturingProcessHandler(this)
//        LOG.info("Executing `$commandLineString`")
//        handler.runProcessWithGlobalProgress(timeoutInMilliseconds)
//    } catch (e: ExecutionException) {
//        LOG.warn("Failed to run executable", e)
//        return null
//    }
//
//    if (!output.isSuccess) {
//        LOG.warn(errorMessage(this, output))
//    }
//
//    return output
//}

fun GeneralCommandLine.execute(): ProcessOutput? {
    LOG.info("Executing `$commandLineString`")
    val handler = MvCapturingProcessHandler.startProcess(this).unwrapOrElse {
        LOG.warn("Failed to run executable", it)
        return null
    }
    val output = handler.runProcessWithGlobalProgress()

    if (!output.isSuccess) {
        LOG.warn(MvProcessExecutionException.errorMessage(commandLineString, output))
    }

    return output
}

private fun errorMessage(commandLine: GeneralCommandLine, output: ProcessOutput): String = """
        |Execution failed (exit code ${output.exitCode}).
        |${commandLine.commandLineString}
        |stdout : ${output.stdout}
        |stderr : ${output.stderr}
    """.trimMargin()

private fun CapturingProcessHandler.runProcessWithGlobalProgress(timeoutInMilliseconds: Int? = null): ProcessOutput {
    return runProcess(ProgressManager.getGlobalProgressIndicator(), timeoutInMilliseconds)
}

private fun CapturingProcessHandler.runProcess(
    indicator: ProgressIndicator?,
    timeoutInMilliseconds: Int? = null
): ProcessOutput {
    return when {
        indicator != null && timeoutInMilliseconds != null ->
            runProcessWithProgressIndicator(indicator, timeoutInMilliseconds)

        indicator != null -> runProcessWithProgressIndicator(indicator)
        timeoutInMilliseconds != null -> runProcess(timeoutInMilliseconds)
        else -> runProcess()
    }
}

val ProcessOutput.isSuccess: Boolean get() = !isTimeout && !isCancelled && exitCode == 0
