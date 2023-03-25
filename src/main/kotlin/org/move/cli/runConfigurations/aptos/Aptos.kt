package org.move.cli.runConfigurations.aptos

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.isDirectory
import org.move.cli.Consts
import org.move.cli.settings.aptosPath
import org.move.cli.settings.isValidExecutable
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode
import org.move.stdext.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class Aptos(val location: Path) {
    fun init(
        project: Project,
        owner: Disposable,
        privateKeyPath: String,
        faucetUrl: String,
        restUrl: String,
    ): MvProcessResult<ProcessOutput> {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val commandLine = AptosCommandLine(
            "init",
            arguments = listOf(
                "--private-key-file", privateKeyPath,
                "--faucet-url", faucetUrl,
                "--rest-url", restUrl,
                "--assume-yes"
            ),
            workingDirectory = project.root
        )
        return commandLine.toGeneralCommandLine(this).execute(owner)
    }

    @Suppress("FunctionName")
    fun move_init(
        project: Project,
        owner: Disposable,
        rootDirectory: VirtualFile,
        packageName: String,
    ): MvProcessResult<VirtualFile> {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val commandLine = AptosCommandLine(
            "move",
            listOf(
                "init",
                "--name", packageName,
                "--assume-yes"
            ),
            workingDirectory = project.root
        )
        commandLine.toGeneralCommandLine(this)
            .execute(owner)
            .unwrapOrElse { return MvResult.Err(it) }
        fullyRefreshDirectory(rootDirectory)

        val manifest =
            checkNotNull(rootDirectory.findChild(Consts.MANIFEST_FILE)) { "Can't find the manifest file" }
        return MvResult.Ok(manifest)
    }

    fun version(): String? {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        if (!location.isValidExecutable()) return null

        val commandLine = AptosCommandLine(
            null,
            listOf("--version"),
            workingDirectory = null,
        )
        val lines = commandLine.toGeneralCommandLine(this).execute()?.stdoutLines.orEmpty()
        return if (lines.isNotEmpty()) return lines.joinToString("\n") else null
    }

    companion object {
        fun fromProject(project: Project): Aptos? = project.aptosPath?.let { Aptos(it) }

        data class GeneratedFilesHolder(val manifest: VirtualFile)

        fun suggestPath(): String? {
            for (path in homePathCandidates()) {
                when {
                    path.isDirectory() -> {
                        val candidate = path.resolveExisting(executableName("aptos")) ?: continue
                        if (candidate.isExecutableFile())
                            return candidate.toAbsolutePath().toString()
                    }
                    path.isExecutableFile() && path.fileName.toString() == executableName("aptos") -> {
                        if (path.isExecutableFile())
                            return path.toAbsolutePath().toString()
                    }
                }
            }
            return null
        }

        private fun homePathCandidates(): Sequence<Path> {
            return System.getenv("PATH")
                .orEmpty()
                .split(File.pathSeparator)
                .asSequence()
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toPathOrNull() }
        }
    }
}
