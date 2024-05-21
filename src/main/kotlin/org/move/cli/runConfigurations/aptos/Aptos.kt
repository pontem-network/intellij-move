package org.move.cli.runConfigurations.aptos

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.execution.ParametersListUtil
import org.move.cli.Consts
import org.move.cli.MoveProject
import org.move.cli.externalLinter.ExternalLinter
import org.move.cli.externalLinter.externalLinterSettings
import org.move.cli.runConfigurations.BlockchainCli
import org.move.cli.runConfigurations.CliCommandLineArgs
import org.move.cli.settings.moveSettings
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode
import org.move.stdext.RsResult
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.stdext.buildList
import org.move.stdext.unwrapOrElse
import java.nio.file.Path
import java.nio.file.Paths

data class Aptos(
    override val cliLocation: Path,
    val parentDisposable: Disposable?
): BlockchainCli(parentDisposable) {

    override fun init(
        project: Project,
        rootDirectory: VirtualFile,
        packageName: String
    ): RsProcessResult<VirtualFile> {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val commandLine = CliCommandLineArgs(
            "move",
            listOf(
                "init",
                "--name", packageName,
                "--assume-yes"
            ),
            workingDirectory = project.rootPath
        )
        executeCommandLine(commandLine).unwrapOrElse { return Err(it) }

        fullyRefreshDirectory(rootDirectory)

        val manifest =
            checkNotNull(rootDirectory.findChild(Consts.MANIFEST_FILE)) { "Can't find the manifest file" }
        return Ok(manifest)
    }

    override fun fetchPackageDependencies(
        project: Project,
        projectDir: Path,
        skipLatest: Boolean,
        processListener: ProcessListener
    ): RsProcessResult<Unit> {
        if (project.moveSettings.fetchAptosDeps) {
            val commandLine =
                CliCommandLineArgs(
                    subCommand = "move",
                    arguments = listOfNotNull(
                        "compile",
                        "--skip-fetch-latest-git-deps".takeIf { skipLatest }
                    ),
                    workingDirectory = projectDir
                )
            // TODO: as Aptos does not yet support fetching dependencies without compiling, ignore errors here,
            // TODO: still better than no call at all
            executeCommandLine(commandLine, listener = processListener)
//                .unwrapOrElse { return Err(it) }
        }
        return Ok(Unit)
    }

    fun checkProject(args: AptosCompileArgs): RsResult<ProcessOutput, RsProcessExecutionException.Start> {
//            val useClippy = args.linter == ExternalLinter.CLIPPY
//                    && !checkNeedInstallClippy(project, args.cargoProjectDirectory)
//            val checkCommand = if (useClippy) "clippy" else "check"
        val extraArguments = ParametersListUtil.parse(args.extraArguments)
        val commandLine =
            CliCommandLineArgs(
                "move compile",
                buildList {
//                        add("--message-format=json")
                    if ("--skip-fetch-latest-git-deps" !in extraArguments) {
                        add("--skip-fetch-latest-git-deps")
                    }
                    if (args.isCompilerV2 && "--compiler-version" !in extraArguments) {
                        add("--compiler-version")
                        add("v2")
                    }
                    if (args.isCompilerV2 && "--language-version" !in extraArguments) {
                        add("--language-version")
                        add("2.0")
                    }
                    addAll(ParametersListUtil.parse(args.extraArguments))
                },
                args.moveProjectDirectory,
                environmentVariables = EnvironmentVariablesData.create(args.envs, true)
            )
        return executeCommandLine(commandLine).ignoreExitCode()
    }

    fun downloadPackage(
        project: Project,
        accountAddress: String,
        packageName: String,
        outputDir: String,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress(timeoutInMilliseconds = null) }
    ): RsProcessResult<ProcessOutput> {
        val commandLine = CliCommandLineArgs(
            subCommand = "move download",
            arguments = buildList {
                add("--account"); add(accountAddress)
                add("--package"); add(packageName)
                add("--bytecode")
                add("--output-dir"); add(outputDir)
            },
            workingDirectory = project.basePath?.let { Path.of(it) },
            environmentVariables = EnvironmentVariablesData.DEFAULT
        )
        return executeCommandLine(commandLine, runner = runner)
    }

    fun decompileDownloadedPackage(downloadedPackagePath: Path): RsProcessResult<ProcessOutput> {
        val bytecodeModulesPath =
            downloadedPackagePath.resolve("bytecode_modules").toAbsolutePath().toString()
        val commandLine = CliCommandLineArgs(
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
        val commandLine = CliCommandLineArgs(
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
}

data class AptosCompileArgs(
    val linter: ExternalLinter,
    val moveProjectDirectory: Path,
    val extraArguments: String,
    val envs: Map<String, String>,
    val isCompilerV2: Boolean,
    val skipLatestGitDeps: Boolean,
) {
    companion object {
        fun forMoveProject(moveProject: MoveProject): AptosCompileArgs {
            val linterSettings = moveProject.project.externalLinterSettings
            val moveSettings = moveProject.project.moveSettings
            return AptosCompileArgs(
                linterSettings.tool,
                moveProject.workingDirectory,
//                moveProject.project.rustSettings.compileAllTargets,
                linterSettings.additionalArguments,
//                settings.channel,
                linterSettings.envs,
                isCompilerV2 = moveSettings.isCompilerV2,
                skipLatestGitDeps = moveSettings.skipFetchLatestGitDeps
            )
        }
    }
}

val MoveProject.workingDirectory: Path get() = this.currentPackage.contentRoot.pathAsPath