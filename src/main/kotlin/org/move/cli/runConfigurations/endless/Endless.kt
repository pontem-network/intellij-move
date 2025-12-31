package org.move.cli.runConfigurations.endless

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.MoveProject
import org.move.cli.MvConstants
import org.move.cli.runConfigurations.EndlessCommandLine
import org.move.cli.settings.moveSettings
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode
import org.move.stdext.RsResult
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.stdext.unwrapOrElse
import java.nio.file.Path
import java.nio.file.Paths

data class Endless(val cliLocation: Path, val parentDisposable: Disposable?): Disposable {

    private val innerDisposable = Disposer.newCheckedDisposable("Endless CLI disposable")

    init {
        Disposer.register(this, innerDisposable)

        if (parentDisposable != null) {
            Disposer.register(parentDisposable, this)
        }
    }

    fun init(
        project: Project,
        rootDirectory: VirtualFile,
        packageName: String
    ): RsProcessResult<VirtualFile> {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val commandLine = EndlessCommandLine(
            "move init",
            EndlessArgs().applyExtra(
                listOf(
                    "--name", packageName,
                    "--assume-yes"
                )
            ),
            workingDirectory = project.rootPath
        )
        executeCommandLine(commandLine).unwrapOrElse { return Err(it) }

        fullyRefreshDirectory(rootDirectory)
        val manifest =
            checkNotNull(rootDirectory.findChild(MvConstants.MANIFEST_FILE)) { "Can't find the manifest file" }

        return Ok(manifest)
    }

    fun checkProject(
        project: Project,
        linterArgs: EndlessExternalLinterArgs
    ): RsResult<ProcessOutput, RsProcessExecutionException.Start> {
        val lintCommand = linterArgs.linter.command

        val endlessArgs = EndlessArgs().applyExtra(linterArgs.extraArguments)

        if (project.isCompilerJsonOutputEnabled) {
            endlessArgs.enableCompilerJson()
        }
        val commandLine =
            EndlessCommandLine(
                "move $lintCommand",
                endlessArgs,
                linterArgs.moveProjectDirectory,
            )
        return executeCommandLine(commandLine).ignoreExitCode()
    }

    fun downloadPackage(
        project: Project,
        accountAddress: String,
        packageName: String,
        outputDir: String,
        profile: String? = null,
        networkUrl: String? = null,
        connectionTimeoutSecs: Int = -1,
        nodeApiKey: String? = null,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress() }
    ): RsProcessResult<ProcessOutput> {
        val commandLine = EndlessCommandLine(
            subCommand = "move download",
            arguments = EndlessArgs().applyExtra {
                add("--account"); add(accountAddress)
                add("--package"); add(packageName)
                add("--bytecode")
                add("--output-dir"); add(outputDir)
                if (!profile.isNullOrBlank()) {
                    add("--profile"); add(profile)
                }
                if (!networkUrl.isNullOrBlank()) {
                    add("--url"); add(networkUrl)
                }
                if (!nodeApiKey.isNullOrBlank()) {
                    add("--node-api-key"); add(nodeApiKey)
                }
                if (connectionTimeoutSecs != -1) {
                    add("--connection-timeout-secs"); add(connectionTimeoutSecs.toString())
                }
            },
            workingDirectory = project.basePath?.let { Path.of(it) },
            environmentVariables = EnvironmentVariablesData.DEFAULT
        )
        return executeCommandLine(commandLine, runner = runner)
    }

    fun decompileDownloadedPackage(downloadedPackagePath: Path): RsProcessResult<ProcessOutput> {
        val bytecodeModulesPath =
            downloadedPackagePath.resolve("bytecode_modules").toAbsolutePath().toString()
        val commandLine = EndlessCommandLine(
            subCommand = "move decompile",
            arguments = EndlessArgs().applyExtra {
                add("--package-path"); add(bytecodeModulesPath)
                add("--assume-yes")
            },
            workingDirectory = downloadedPackagePath
        )
        return executeCommandLine(commandLine)
    }

    fun decompileFile(
        bytecodeFilePath: String,
        outputDir: String?,
    ): RsProcessResult<ProcessOutput> {
        val fileRoot = Paths.get(bytecodeFilePath).parent
        val commandLine = EndlessCommandLine(
            subCommand = "move decompile",
            arguments = EndlessArgs().applyExtra {
                add("--bytecode-path"); add(bytecodeFilePath)
                if (outputDir != null) {
                    add("--output-dir"); add(outputDir)
                }
                add("--assume-yes")
            },
            workingDirectory = fileRoot
        )
        // only one second is allowed to run decompiler, otherwise fails with timeout
        return executeCommandLine(commandLine)
    }

    private fun executeCommandLine(
        commandLine: EndlessCommandLine,
        colored: Boolean = false,
        listener: ProcessListener? = null,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress() }
    ): RsProcessResult<ProcessOutput> {
        val generalCommandLine =
            commandLine.toGeneralCommandLine(this.cliLocation, emulateTerminal = colored)
        return generalCommandLine.execute(innerDisposable, runner, listener)
    }

    override fun dispose() {}
}

val Project.isCompilerJsonOutputEnabled
    get() = this.moveSettings.enableMove2
            && AdvancedSettings.getBoolean("org.move.endless.compile.message.json")

val MoveProject.workingDirectory: Path get() = this.currentPackage.contentRoot.pathAsPath
