package org.move.cli

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode
import org.move.stdext.MvResult
import org.move.stdext.unwrapOrElse
import java.nio.file.Path
import kotlin.io.path.notExists

class Aptos(private val exePath: Path) {

    fun init(
        project: Project,
        owner: Disposable,
        directory: VirtualFile,
        name: String,
    ): MvProcessResult<GeneratedFilesHolder> {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val path = directory.pathAsPath
        val commandLine = GeneralCommandLine(exePath.toString(), "move", "init")
            .withWorkDirectory(project.contentRoots.firstOrNull()?.toNioPathOrNull())
            .withParameters(listOf("--package-dir", path.toString(), "--name", name, "--framework-dir", "."))
            .withEnvironment(emptyMap())
            .withCharset(Charsets.UTF_8)
        commandLine.execute(owner).unwrapOrElse { return MvResult.Err(it) }
        fullyRefreshDirectory(directory)

        val manifest =
            checkNotNull(directory.findChild(MoveConstants.MANIFEST_FILE)) { "Can't find the manifest file" }
        return MvResult.Ok(GeneratedFilesHolder(manifest))
    }

    fun version(workingDirectory: Path? = null): String? {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        if (exePath.notExists()) return null

        val path = exePath.toString()
        if (StringUtil.isEmptyOrSpaces(path)) return null

        val commandLine = GeneralCommandLine(path)
            .withWorkDirectory(workingDirectory)
            .withParameters(listOf("--version"))
            .withEnvironment(emptyMap())
            .withCharset(Charsets.UTF_8)
        val lines = commandLine.execute()?.stdoutLines.orEmpty()
        return if (lines.isNotEmpty()) return lines.joinToString("\n") else null
//        try {
//            val execPath = aptosPath
//            val cwd =
//            val cwd = project.contentRoots.firstOrNull()?.toNioPathOrNull() ?: return null


//            val (out, err) = runExecutable(aptosPath, workingDirectory, "--version")
//            if (err.isNotEmpty()) return null
//            return out
//        } catch (e: ProcessNotCreatedException) {
//            return null
//        }
    }

//    private fun runExecutable(execPath: Path, cwd: Path?, vararg command: String): Pair<String, String> {
//        val workingDirectory = cwd?.toFile()
//        val process =
//            GeneralCommandLine(execPath.toAbsolutePath().toString(), *command)
//                .withWorkDirectory(workingDirectory)
//                .createProcess()
//        LOG.info("Executing command at $cwd: ${process.info().commandLine()}")
//
//        val out = CharStreams.toString(InputStreamReader(process.inputStream))
//        val err = CharStreams.toString(InputStreamReader(process.errorStream))
//        return Pair(out, err)
//    }

    companion object {
//        private val LOG = logger<Aptos>()

        data class GeneratedFilesHolder(val manifest: VirtualFile)
    }
}
