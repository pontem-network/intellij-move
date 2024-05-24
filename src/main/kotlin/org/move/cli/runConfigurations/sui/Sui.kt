package org.move.cli.runConfigurations.sui

import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.Consts
import org.move.cli.runConfigurations.BlockchainCli
import org.move.cli.runConfigurations.CliCommandLineArgs
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.stdext.unwrapOrElse
import java.nio.file.Path

data class Sui(override val cliLocation: Path, val parentDisposable: Disposable?):
    BlockchainCli(parentDisposable) {
    override fun init(
        project: Project,
        rootDirectory: VirtualFile,
        packageName: String
    ): RsProcessResult<VirtualFile> {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val commandLine =
            CliCommandLineArgs(
                "move new",
                arguments = listOf("--path", "."),
                workingDirectory = project.rootPath
            )
        executeCommandLine(commandLine)
            .unwrapOrElse { return Err(it) }

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
        val commandLine =
            CliCommandLineArgs(
                subCommand = "move build",
                arguments = listOfNotNull(
                    "--fetch-deps-only",
                    "--skip-fetch-latest-git-deps".takeIf { skipLatest }),
                workingDirectory = projectDir
            )
        executeCommandLine(commandLine, listener = processListener)
            .unwrapOrElse { return Err(it) }
        return Ok(Unit)
    }
}
