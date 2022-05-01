package org.move.cli

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.text.StringUtil
import org.move.openapiext.checkIsBackgroundThread
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.execute
import org.move.openapiext.withWorkDirectory
import java.nio.file.Path
import kotlin.io.path.notExists

class Aptos(private val exePath: Path) {

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

//    companion object {
//        private val LOG = logger<Aptos>()
//    }
}
