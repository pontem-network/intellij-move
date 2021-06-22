package org.move.cli

import com.google.common.io.CharStreams
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.move.ide.notifications.MoveNotifications
import org.move.utils.rootService
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Path

class DoveExecutable(private val project: Project, private val dovePath: Path) {
    private fun runExecutable(cwd: File, vararg command: String): Pair<String, String> {
        val process =
            GeneralCommandLine(dovePath.toAbsolutePath().toString(), *command)
                .withWorkDirectory(cwd)
                .createProcess()
        val out = CharStreams.toString(InputStreamReader(process.inputStream))
        val err = CharStreams.toString(InputStreamReader(process.errorStream))
        return Pair(out, err)
    }

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

    fun tomlValidate(doveProjectRoot: Path): ValidationOutput? {
        try {
            val (out, err) = runExecutable(doveProjectRoot.toFile(), "metadata", "--validate")
            if (err.isNotEmpty()) {
                MoveNotifications.pluginNotifications()
                    .createNotification(err, NotificationType.ERROR)
                    .notify(project)
                return null
            }
            return try {
                Gson().fromJson(out, ValidationOutput::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        } catch (e: ProcessNotCreatedException) {
            return null
        }
    }

    fun metadata(doveProjectRoot: Path): DoveProjectMetadata? {
        try {
            val (out, err) = runExecutable(doveProjectRoot.toFile(), "metadata")
            if (err.isNotEmpty()) {
                MoveNotifications.pluginNotifications()
                    .createNotification(err, NotificationType.ERROR)
                    .notify(project)
                return null
            }
            return try {
                Gson().fromJson(out, DoveProjectMetadata::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        } catch (e: ProcessNotCreatedException) {
            return null
        }
    }
}
