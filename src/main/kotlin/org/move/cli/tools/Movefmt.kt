package org.move.cli.tools

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.containers.addAllIfNotNull
import org.move.openapiext.RsProcessResult
import org.move.openapiext.execute
import org.move.openapiext.runProcessWithGlobalProgress
import java.io.File
import java.nio.file.Path


class Movefmt(val cliLocation: Path, val parentDisposable: Disposable): Disposable.Default {

    // cannot make Movefmt CheckedDisposable, need to use another one
    private val innerDisposable = Disposer.newCheckedDisposable("Movefmt disposable")

    init {
        Disposer.register(this, innerDisposable)
        Disposer.register(parentDisposable, this)
    }

    fun reformatFile(
        file: File,
        additionalArguments: List<String>,
        workingDirectory: Path,
        envs: EnvironmentVariablesData,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress() }
    ): RsProcessResult<ProcessOutput> {
        val commandLine = MvCommandLine(
            arguments = buildList {
                add("-q")
                addAllIfNotNull("--emit", "stdout")
                if (additionalArguments.isNotEmpty()) {
                    addAll(additionalArguments)
                }
                add(file.absolutePath)
            },
            workingDirectory = workingDirectory,
            environmentVariables = envs.with(mapOf("MOVEFMT_LOG" to "error")),
        )
        return commandLine
            .toGeneralCommandLine(this.cliLocation)
            // needs to skip stderr here
            .withRedirectErrorStream(false)
            .execute(
                innerDisposable,
                runner,
            )
    }
}