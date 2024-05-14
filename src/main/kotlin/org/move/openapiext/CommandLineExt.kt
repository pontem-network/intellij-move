package org.move.openapiext

/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.systemIndependentPath
import org.move.cli.runConfigurations.MvCapturingProcessHandler
import org.move.cli.settings.isDebugModeEnabled
import org.move.ide.notifications.showBalloonWithoutProject
import org.move.stdext.RsResult
import org.move.stdext.unwrapOrElse
import java.nio.file.Path

private val LOG = Logger.getInstance("org.move.openapiext.CommandLineExt")

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
    showCommandLineBalloon(commandLineString, output)

    if (!output.isSuccess) {
        LOG.warn(RsProcessExecutionException.errorMessage(commandLineString, output))
    }
    return output
}

/// `owner` parameter represents the object whose lifetime it's using for the process lifetime
fun GeneralCommandLine.execute(
    owner: Disposable,
    stdIn: ByteArray? = null,
    runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress(timeoutInMilliseconds = null) },
    listener: ProcessListener? = null
): RsProcessResult<ProcessOutput> {
    LOG.info("Executing `$commandLineString`")

    val processHandler = MvCapturingProcessHandler
        .startProcess(this) // The OS process is started here
        .unwrapOrElse {
            LOG.warn("Failed to run executable", it)
            return RsResult.Err(RsProcessExecutionException.Start(commandLineString, it))
        }

    // kill process on dispose()
    val processKiller = Disposable {
        // Don't attempt a graceful termination, Cargo can be SIGKILLed safely.
        // https://github.com/rust-lang/cargo/issues/3566
        if (!processHandler.isProcessTerminated) {
            processHandler.process.destroyForcibly() // Send SIGKILL
            processHandler.destroyProcess()
        }
    }

    @Suppress("DEPRECATION")
    val ownerIsAlreadyDisposed =
        runReadAction {
            // check that owner is disposed, kill process then
            if (Disposer.isDisposed(owner)) {
                true
            } else {
                Disposer.register(owner, processKiller)
                false
            }
        }
    if (ownerIsAlreadyDisposed) {
        Disposer.dispose(processKiller) // Kill the process
        // On the one hand, this seems fishy,
        // on the other hand, this is isomorphic
        // to the scenario where cargoKiller triggers.
        val output = ProcessOutput().apply { setCancelled() }
        return RsResult.Err(
            RsProcessExecutionException.Canceled(
                commandLineString,
                output,
                "Command failed to start"
            )
        )
    }

    listener?.let { processHandler.addProcessListener(it) }

    val output = try {
        if (stdIn != null) {
            processHandler.processInput.use { it.write(stdIn) }
        }

        processHandler.runner()
    } finally {
        Disposer.dispose(processKiller)
    }
    showCommandLineBalloon(commandLineString, output)

    return when {
        output.isCancelled -> RsResult.Err(RsProcessExecutionException.Canceled(commandLineString, output))
        output.isTimeout -> RsResult.Err(RsProcessExecutionException.Timeout(commandLineString, output))
        output.exitCode != 0 -> RsResult.Err(
            RsProcessExecutionException.ProcessAborted(
                commandLineString,
                output
            )
        )
        else -> RsResult.Ok(output)
    }
}

private fun showCommandLineBalloon(commandLineString: String, output: ProcessOutput) {
    if (isDebugModeEnabled()) {
        val content =
            if (!output.isSuccess) {
                """`$commandLineString` |Execution failed (exit code ${output.exitCode}).
    """
            } else {
                """`$commandLineString` |Execution successful.
    """
            }
        showBalloonWithoutProject(content.trimStart(), INFORMATION)
    }
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
