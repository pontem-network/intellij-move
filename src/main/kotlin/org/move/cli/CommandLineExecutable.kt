package org.move.cli

import com.google.common.io.CharStreams
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.project.Project
import org.move.utils.rootService
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Path

class CommandLineExecutable(private val project: Project, private val executablePath: Path) {
    fun version(): String? {
        try {
            val executablePath = project.rootService.path ?: return null
            val (out, err) = runExecutable(executablePath.toFile(), "--version")
            if (err.isNotEmpty()) return null
            return out.split(' ').last()
        } catch (e: ProcessNotCreatedException) {
            return null
        }
    }

    private fun runExecutable(cwd: File, vararg command: String): Pair<String, String> {
        val process =
            GeneralCommandLine(executablePath.toAbsolutePath().toString(), *command)
                .withWorkDirectory(cwd)
                .createProcess()
        val out = CharStreams.toString(InputStreamReader(process.inputStream))
        val err = CharStreams.toString(InputStreamReader(process.errorStream))
        return Pair(out, err)
    }
}
