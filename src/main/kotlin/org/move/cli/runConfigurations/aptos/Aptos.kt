package org.move.cli.runConfigurations.aptos

import com.fasterxml.jackson.core.JacksonException
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.execution.ParametersListUtil
import org.move.cli.MoveProject
import org.move.cli.MvConstants
import org.move.cli.runConfigurations.AptosCommandLine
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode
import org.move.stdext.RsResult
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.stdext.buildList
import org.move.stdext.unwrapOrElse
import java.nio.file.Path
import java.nio.file.Paths

data class Aptos(val cliLocation: Path, val parentDisposable: Disposable?): Disposable {

    private val innerDisposable = Disposer.newCheckedDisposable("Aptos CLI disposable")

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
        val commandLine = AptosCommandLine(
            "move init",
            listOf(
                "--name", packageName,
                "--assume-yes"
            ),
            workingDirectory = project.rootPath
        )
        executeCommandLine(commandLine).unwrapOrElse { return Err(it) }

        fullyRefreshDirectory(rootDirectory)
        val manifest =
            checkNotNull(rootDirectory.findChild(MvConstants.MANIFEST_FILE)) { "Can't find the manifest file" }

        return Ok(manifest)
    }

    fun fetchPackageDependencies(
        projectDir: Path,
        skipLatest: Boolean,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress() }
    ): AptosProcessResult<Unit> {
        val commandLine =
            AptosCommandLine(
                subCommand = "move compile",
                arguments = listOfNotNull(
                    "--skip-fetch-latest-git-deps".takeIf { skipLatest }
                ),
                workingDirectory = projectDir
            )
        return executeAptosCommandLine(commandLine, colored = true, runner = runner)
    }

    fun checkProject(args: AptosCompileArgs): RsResult<ProcessOutput, RsProcessExecutionException.Start> {
//            val useClippy = args.linter == ExternalLinter.CLIPPY
//                    && !checkNeedInstallClippy(project, args.cargoProjectDirectory)
//            val checkCommand = if (useClippy) "clippy" else "check"
        val extraArguments = ParametersListUtil.parse(args.extraArguments)
        val commandLine =
            AptosCommandLine(
                "move compile",
                buildList {
//                        add("--message-format=json")
                    if ("--skip-fetch-latest-git-deps" !in extraArguments) {
                        add("--skip-fetch-latest-git-deps")
                    }
                    if (args.enableMove2) {
                        if ("--move-2" !in extraArguments) {
                            add("--move-2")
                        }
                    }
                    addAll(ParametersListUtil.parse(args.extraArguments))
                },
                args.moveProjectDirectory,
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
        val commandLine = AptosCommandLine(
            subCommand = "move download",
            arguments = buildList {
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
        val commandLine = AptosCommandLine(
            subCommand = "move decompile",
            arguments = buildList {
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
        val commandLine = AptosCommandLine(
            subCommand = "move decompile",
            arguments = buildList {
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
        commandLine: AptosCommandLine,
        colored: Boolean = false,
        listener: ProcessListener? = null,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress() }
    ): RsProcessResult<ProcessOutput> {
        val generalCommandLine =
            commandLine.toGeneralCommandLine(this.cliLocation, emulateTerminal = colored)
        return generalCommandLine.execute(innerDisposable, runner, listener)
    }

    private fun executeAptosCommandLine(
        commandLine: AptosCommandLine,
        colored: Boolean = false,
        listener: ProcessListener? = null,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress() }
    ): AptosProcessResult<Unit> {
        val processOutput = executeCommandLine(commandLine, colored, listener, runner)
            .ignoreNonZeroExitCode()
            .unwrapOrElse {
                return Err(it)
            }

        val json = processOutput.stdout
            .lines().dropWhile { l -> !l.startsWith("{") }.joinToString("\n").trim()
        val exitStatus = try {
            AptosExitStatus.fromJson(json)
        } catch (e: JacksonException) {
            return Err(RsDeserializationException(e))
        }
        val aptosProcessOutput = AptosProcessOutput(Unit, processOutput, exitStatus)

        return Ok(aptosProcessOutput)
    }

    override fun dispose() {}
}


val MoveProject.workingDirectory: Path get() = this.currentPackage.contentRoot.pathAsPath