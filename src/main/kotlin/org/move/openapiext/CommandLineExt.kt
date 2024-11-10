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
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.SystemProperties
import org.move.cli.runConfigurations.MvCapturingProcessHandler
import org.move.cli.settings.isDebugModeEnabled
import org.move.ide.notifications.showBalloonWithoutProject
import org.move.stdext.RsResult
import org.move.stdext.unwrapOrElse

fun GeneralCommandLine.execute(): ProcessOutput? {
    val processHandler = createProcessHandler().unwrapOrNull() ?: return null

    val output = processHandler.runProcessWithGlobalProgress()
    if (isDebugModeEnabled()) {
        showCommandLineBalloon(output)
    }

    if (!output.isSuccess) {
        LOG.warn(RsProcessExecutionException.errorMessage(commandLineString, output))
    }
    return output
}

/// `owner` parameter represents the object whose lifetime it's using for the process lifetime
fun GeneralCommandLine.execute(
    owner: CheckedDisposable,
    runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress() },
    listener: ProcessListener? = null
): RsProcessResult<ProcessOutput> {
    val processHandler = createProcessHandler().unwrapOrElse { return RsResult.Err(it) }

    // kill process on dispose()
    val processKiller = Disposable {
        // Don't attempt a graceful termination, Cargo can be SIGKILLed safely.
        // https://github.com/rust-lang/cargo/issues/3566
        if (!processHandler.isProcessTerminated) {
            processHandler.process.destroyForcibly() // Send SIGKILL
            processHandler.destroyProcess()
        }
    }

    val ownerIsAlreadyDisposed =
        runReadAction {
            // check that owner is disposed, kill process then
            if (owner.isDisposed) {
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
        // execution happens here
        processHandler.runner()
    } finally {
        Disposer.dispose(processKiller)
    }

    if (isDebugModeEnabled()) {
        showCommandLineBalloon(output)
    }

    return when {
        output.isCancelled -> RsResult.Err(RsProcessExecutionException.Canceled(commandLineString, output))
        output.isTimeout -> RsResult.Err(RsProcessExecutionException.Timeout(commandLineString, output))
        output.exitCode != 0 -> RsResult.Err(
            RsProcessExecutionException.FailedWithNonZeroExitCode(
                commandLineString,
                output
            )
        )
        else -> RsResult.Ok(output)
    }
}

private fun GeneralCommandLine.createProcessHandler(): RsProcessResult<MvCapturingProcessHandler> {
    LOG.info("Executing `$commandLineString`")
    val processHandler = MvCapturingProcessHandler.startProcess(this)
        .mapErr {
            LOG.warn("Failed to run executable", it)
            return RsResult.Err(RsProcessExecutionException.Start(commandLineString, it))
        }
    return processHandler
}

fun CapturingProcessHandler.runProcessWithGlobalProgress(timeoutInMilliseconds: Int? = null): ProcessOutput {
    // indicator can be null if ProgressManager not yet initialized
    val indicator = ProgressManager.getGlobalProgressIndicator()
    return when {
        indicator != null && timeoutInMilliseconds != null ->
            runProcessWithProgressIndicator(indicator, timeoutInMilliseconds)

        indicator != null -> runProcessWithProgressIndicator(indicator)
        timeoutInMilliseconds != null -> runProcess(timeoutInMilliseconds)
        else -> runProcess()
    }
}

private fun GeneralCommandLine.showCommandLineBalloon(processOutput: ProcessOutput) {
    val workingDirectory = this.workingDirectory?.toString()
    val userHome = SystemProperties.getUserHome()
    val command = commandLineString.replace(userHome, "~")
    val workDir = workingDirectory?.replace(userHome, "~")
    val atWorkDir = if (workDir != null) "&nbsp;<p>at $workDir</p>" else ""
    when {
        processOutput.isTimeout -> {
            showBalloonWithoutProject(
                "Execution failed",
                "<code>$command</code> (timeout) $atWorkDir",
                INFORMATION
            )
        }
        processOutput.isCancelled -> {
            showBalloonWithoutProject(
                "Execution failed",
                "<code>$command</code> (cancelled) $atWorkDir",
                INFORMATION
            )
        }
        processOutput.exitCode != 0 -> {
            showBalloonWithoutProject(
                "Execution failed",
                "<code>$command</code> (exit code ${processOutput.exitCode}) $atWorkDir " +
                        "<p>stdout=${processOutput.stdout}</p>" +
                        "<p>stderr=${processOutput.stderr}</p>",
                INFORMATION
            )
        }
        else -> {
            showBalloonWithoutProject(
                "Execution successful",
                "<code>$command</code> $atWorkDir",
                INFORMATION
            )
        }
    }
}

private val LOG = Logger.getInstance("org.move.openapiext.CommandLineExt")

