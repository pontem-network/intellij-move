package org.move.cli

import com.google.common.io.CharStreams
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.move.lang.toNioPathOrNull
import org.move.openapiext.contentRoots
import org.move.settings.moveBinaryPath
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Path

class MoveBinary(
    private val project: Project,
    private val path: Path? = project.moveBinaryPath
) {
    fun version(): String? {
        try {
            val execPath = path ?: return null
            val cwd = project.contentRoots.firstOrNull()?.toNioPathOrNull() ?: return null

            val (out, err) = runExecutable(execPath, cwd.toFile(), "--version")
            if (err.isNotEmpty()) return null
            return out
        } catch (e: ProcessNotCreatedException) {
            return null
        }
    }

    private fun runExecutable(execPath: Path, cwd: File, vararg command: String): Pair<String, String> {
        val process =
            GeneralCommandLine(execPath.toAbsolutePath().toString(), *command)
                .withWorkDirectory(cwd)
                .createProcess()
        LOG.info("Executing command at $cwd: ${process.info().commandLine()}")

        val out = CharStreams.toString(InputStreamReader(process.inputStream))
        val err = CharStreams.toString(InputStreamReader(process.errorStream))
        return Pair(out, err)
    }

    companion object {
        private val LOG = logger<MoveBinary>()
    }
}
