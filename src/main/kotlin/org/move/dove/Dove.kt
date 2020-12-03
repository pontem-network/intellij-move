package org.move.dove

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.util.io.exists
import org.move.openapiext.execute
import org.move.openapiext.rootDir
import org.move.project.settings.moveSettings
import java.nio.file.Path

object Dove {
    fun fetchPackageMetadata(project: Project, rootDir: Path = project.rootDir): DoveMetadata.Package {
        val metadataJson = fetchMetadataJson(project, rootDir)
        return Gson().fromJson(
            metadataJson.getAsJsonObject("package"),
            DoveMetadata.Package::class.java
        )
    }

    fun fetchLayoutMetadata(project: Project, rootDir: Path = project.rootDir): DoveMetadata.Layout {
        val metadataJson = fetchMetadataJson(project, rootDir)
        return Gson().fromJson(
            metadataJson.getAsJsonObject("layout"),
            DoveMetadata.Layout::class.java
        )
    }

    private fun fetchMetadataJson(project: Project, rootDir: Path): JsonObject {
        if (!rootDir.exists()) error("Working directory `$rootDir` does not exist.")
        val doveExecutable = project.moveSettings.doveExecutable ?: error("`doveExecutable` is not set.")
        val command = command(
            doveExecutable,
            rootDir,
            "metadata",
            listOf("--json"))
        val out =
            command.execute()?.stdout ?: error("Command ${command.commandLineString} cannot be executed.")
        return JsonParser.parseString(out).asJsonObject
    }
}
