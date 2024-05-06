package org.move.cli.runConfigurations

import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.Consts
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.stdext.unwrapOrElse
import java.nio.file.Path

sealed class BlockchainCli {

    abstract val cliLocation: Path

    abstract fun init(
        project: Project,
        parentDisposable: Disposable,
        rootDirectory: VirtualFile,
        packageName: String,
    ): MvProcessResult<VirtualFile>

    abstract fun fetchPackageDependencies(
        projectDir: Path,
        skipLatest: Boolean,
        owner: Disposable,
        processListener: ProcessListener
    ): MvProcessResult<Unit>

    data class Aptos(override val cliLocation: Path): BlockchainCli() {
        override fun init(
            project: Project,
            parentDisposable: Disposable,
            rootDirectory: VirtualFile,
            packageName: String
        ): MvProcessResult<VirtualFile> {
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
            commandLine
                .toGeneralCommandLine(cliLocation)
                .execute(parentDisposable)
                .unwrapOrElse { return Err(it) }
            fullyRefreshDirectory(rootDirectory)

            val manifest =
                checkNotNull(rootDirectory.findChild(Consts.MANIFEST_FILE)) { "Can't find the manifest file" }
            return Ok(manifest)
        }

        override fun fetchPackageDependencies(
            projectDir: Path,
            skipLatest: Boolean,
            owner: Disposable,
            processListener: ProcessListener
        ): MvProcessResult<Unit> {
            if (Registry.`is`("org.move.aptos.fetch.deps")) {
                val cli =
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
                cli.toGeneralCommandLine(cliLocation)
                    .execute(owner, listener = processListener)
//                .unwrapOrElse { return Err(it) }
            }
            return Ok(Unit)
        }
    }

    data class Sui(override val cliLocation: Path): BlockchainCli() {
        override fun init(
            project: Project,
            parentDisposable: Disposable,
            rootDirectory: VirtualFile,
            packageName: String
        ): MvProcessResult<VirtualFile> {
            if (!isUnitTestMode) {
                checkIsBackgroundThread()
            }
            val commandLine = CliCommandLineArgs(
                "move",
                listOf(
                    "new", packageName,
                    "--path", "."
                ),
                workingDirectory = project.rootPath
            )
            commandLine.toGeneralCommandLine(this.cliLocation)
                .execute(parentDisposable)
                .unwrapOrElse { return Err(it) }
            fullyRefreshDirectory(rootDirectory)

            val manifest =
                checkNotNull(rootDirectory.findChild(Consts.MANIFEST_FILE)) { "Can't find the manifest file" }
            return Ok(manifest)
        }

        override fun fetchPackageDependencies(
            projectDir: Path,
            skipLatest: Boolean,
            owner: Disposable,
            processListener: ProcessListener
        ): MvProcessResult<Unit> {
            val cli =
                CliCommandLineArgs(
                    subCommand = "move",
                    arguments = listOfNotNull(
                        "build",
                        "--fetch-deps-only",
                        "--skip-fetch-latest-git-deps".takeIf { skipLatest }),
                    workingDirectory = projectDir
                )
            cli.toGeneralCommandLine(cliLocation)
                .execute(owner, listener = processListener)
                .unwrapOrElse { return Err(it) }
            return Ok(Unit)
        }
    }
}
