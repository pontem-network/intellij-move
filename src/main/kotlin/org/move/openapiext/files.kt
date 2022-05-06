package org.move.openapiext

import com.intellij.openapi.project.Project
import org.move.cli.MoveProjectsServiceImpl
import org.move.cli.moveProjects
import org.move.cli.processAllMvFilesOnce
import org.move.lang.MoveFile
import org.move.lang.toMoveFile

fun Project.allMoveFilesForContentRoots(): List<MoveFile> {
    val files = mutableListOf<MoveFile>()
    processAllMvFilesOnce(
        (this.moveProjects as MoveProjectsServiceImpl).projects.currentState
    ) { file, _ ->
        val mvFile = file.toMoveFile(this) ?: return@processAllMvFilesOnce
        files.add(mvFile)
    }
    return files
}
