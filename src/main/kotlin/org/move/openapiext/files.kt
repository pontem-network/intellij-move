package org.move.openapiext

import com.intellij.openapi.project.Project
import org.move.cli.MoveProjectsServiceImpl
import org.move.cli.moveProjectsService
import org.move.cli.processAllMoveFilesOnce
import org.move.lang.MoveFile
import org.move.lang.toMoveFile

fun Project.allMoveFilesForContentRoots(): List<MoveFile> {
    val files = mutableListOf<MoveFile>()
    processAllMoveFilesOnce(
        (this.moveProjectsService as MoveProjectsServiceImpl).projects.currentState
    ) { file, _ ->
        val moveFile = file.toMoveFile(this) ?: return@processAllMoveFilesOnce
        files.add(moveFile)
    }
    return files
}
