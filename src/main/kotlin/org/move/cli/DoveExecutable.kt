package org.move.cli

import com.google.common.io.CharStreams
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import org.move.project.configurable.pathToDoveExecutable
import java.io.InputStreamReader

class DoveExecutable(private val project: Project) {
    private fun runExecutable(vararg command: String): Pair<String, String> {
        val projectRoot = project.basePath
        val process =
            GeneralCommandLine(project.pathToDoveExecutable(), *command)
                    .withWorkDirectory(projectRoot)
                    .createProcess()
        val out = CharStreams.toString(InputStreamReader(process.inputStream))
        val err = CharStreams.toString(InputStreamReader(process.errorStream))
        return Pair(out, err)
    }

    fun metadata(): DoveProjectMetadata? {
        val (out, err) = runExecutable("metadata", "--json")
        if (err.isNotEmpty()) {
            return null
        }
        return try {
            Gson().fromJson(out, DoveProjectMetadata::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
}