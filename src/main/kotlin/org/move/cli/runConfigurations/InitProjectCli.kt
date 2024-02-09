package org.move.cli.runConfigurations

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.Consts
import org.move.cli.settings.Blockchain
import org.move.cli.settings.aptos.AptosExec
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode
import org.move.stdext.RsResult
import org.move.stdext.unwrapOrElse
import java.nio.file.Path

sealed class InitProjectCli {
    abstract fun init(
        project: Project,
        parentDisposable: Disposable,
        rootDirectory: VirtualFile,
        packageName: String,
    ): MvProcessResult<VirtualFile>

    data class Aptos(val aptosExec: AptosExec): InitProjectCli() {
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
            val aptosPath = this.aptosExec.toPathOrNull() ?: error("unreachable")
            commandLine.toGeneralCommandLine(aptosPath)
                .execute(parentDisposable)
                .unwrapOrElse { return RsResult.Err(it) }
            fullyRefreshDirectory(rootDirectory)

            val manifest =
                checkNotNull(rootDirectory.findChild(Consts.MANIFEST_FILE)) { "Can't find the manifest file" }
            return RsResult.Ok(manifest)
        }
    }

    data class Sui(val cliLocation: Path): InitProjectCli() {
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
                .unwrapOrElse { return RsResult.Err(it) }
            fullyRefreshDirectory(rootDirectory)

            val manifest =
                checkNotNull(rootDirectory.findChild(Consts.MANIFEST_FILE)) { "Can't find the manifest file" }
            return RsResult.Ok(manifest)
        }
    }
}
