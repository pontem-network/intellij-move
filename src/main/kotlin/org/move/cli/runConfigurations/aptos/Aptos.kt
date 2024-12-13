package org.move.cli.runConfigurations.aptos

import com.fasterxml.jackson.core.JacksonException
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.execution.ParametersListUtil
import org.move.cli.MoveProject
import org.move.cli.MvConstants
import org.move.cli.externalLinter.ExternalLinter
import org.move.cli.runConfigurations.AptosCommandLine
import org.move.cli.settings.moveSettings
import org.move.cli.update.AptosTool
import org.move.cli.update.UpdateCheckResult
import org.move.cli.update.UpdateCheckResult.*
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
        project: Project,
        packageRoot: Path,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress() }
    ): AptosProcessResult<Unit> {
        val commandLine =
            AptosCommandLine(
                subCommand = "move compile",
                arguments = compilerArguments(project),
                workingDirectory = packageRoot
            )
        return executeAptosCommandLine(commandLine, colored = true, runner = runner)
    }

    fun checkProject(
        project: Project,
        linterArgs: AptosExternalLinterArgs
    ): RsResult<ProcessOutput, RsProcessExecutionOrDeserializationException> {
        val lintCommand = linterArgs.linter.command
        val extraArguments = ParametersListUtil.parse(linterArgs.extraArguments)
        val arguments = when (linterArgs.linter) {
            ExternalLinter.COMPILER -> compilerArguments(project, extraArguments)
            ExternalLinter.LINTER -> {
                buildList {
                    addAll(extraArguments)
                    if (project.moveSettings.skipFetchLatestGitDeps
                        && "--skip-fetch-latest-git-deps" !in extraArguments
                    ) {
                        add("--skip-fetch-latest-git-deps")
                    }
                }
            }
        }
        val commandLine =
            AptosCommandLine(
                "move $lintCommand",
                arguments,
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

    fun checkForToolUpdate(tool: AptosTool): RsResult<UpdateCheckResult, RsProcessExecutionOrDeserializationException> {
        val commandLine =
            AptosCommandLine(
                subCommand = "update",
                arguments = buildList { add(tool.id); add("--check") },
                workingDirectory = null
            )
        val processOutput = executeAptosCommandLine(commandLine)
            .unwrapOrElse { return Err(it) }

        val checkResult = when (val exitStatus = processOutput.exitStatus) {
            is AptosExitStatus.Result -> {
                val message = exitStatus.message
                run {
                    when {
                        message.startsWith("Update is available") -> {
                            val match = APTOS_VERSION_REGEX.find(message) ?: return@run MalformedResult(message)
                            UpdateIsAvailable(match.value)
                        }
                        message.startsWith("Already up to date") -> {
                            val match = APTOS_VERSION_REGEX.find(message) ?: return@run MalformedResult(message)
                            UpToDate(match.value)
                        }
                        else -> MalformedResult(message)
                    }
                }
            }
            is AptosExitStatus.Error -> UpdateError(exitStatus.message)
            is AptosExitStatus.Malformed -> MalformedResult(exitStatus.message)
        }
        return Ok(checkResult)
    }

    fun doToolUpdate(tool: AptosTool, processListener: ProcessListener): AptosProcessResult<Unit> {
        val commandLine =
            AptosCommandLine(
                subCommand = "update",
                arguments = buildList { add(tool.id) },
                workingDirectory = null
            )
        val yesNoListener = object: ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (event.text.contains("Do you want to continue? [Y/n]")) {
                    val handler = event.processHandler
                    handler.processInput?.use { it.write("Y".toByteArray()) }
                }
            }
        }
        val compositeListener = CompositeProcessListener(yesNoListener, processListener)
        val output = executeAptosCommandLine(commandLine, listener = compositeListener)
            .unwrapOrElse {
                return Err(it)
            }
        return Ok(output)
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

    private fun compilerArguments(
        project: Project,
        extraArguments: List<String> = emptyList(),
    ): List<String> {
        val settings = project.moveSettings
        return buildList {
            addAll(extraArguments)
            if (settings.enableMove2 && "--move-2" !in extraArguments) {
                add("--move-2")
            }
            if (settings.skipFetchLatestGitDeps && "--skip-fetch-latest-git-deps" !in extraArguments) {
                add("--skip-fetch-latest-git-deps")
            }
        }
    }

    override fun dispose() {}
}


val MoveProject.workingDirectory: Path get() = this.currentPackage.contentRoot.pathAsPath

val APTOS_VERSION_REGEX = Regex("""\d+.\d+.\d""")