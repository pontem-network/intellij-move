package org.move.movec

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import org.move.movec.cli.MovecCommandLine
import java.nio.file.Path

//class Movec(private val executable: Path) {
//    @Throws(ExecutionException::class)
//    private fun fetchMetadata(owner: Project, projectDirectory: Path): MovecMetadata.Project {
//        val json = MovecCommandLine(executable, "metadata", projectDirectory)
//            .execute(owner)
//            .stdout
//            .dropWhile { it != '{' }
//        val project = try {
//            Gson().fromJson(json, MovecMetadata.Project::class.java)
//        } catch (e: JsonSyntaxException) {
//            throw ExecutionException(e)
//        }
//        return project
//    }
//}