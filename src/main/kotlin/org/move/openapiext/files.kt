package org.move.openapiext

import com.intellij.openapi.project.Project
import org.move.cli.MoveProjectsServiceImpl
import org.move.cli.moveProjectsService
import org.move.cli.processAllMvFilesOnce
import org.move.lang.MvFile
import org.move.lang.toMvFile

fun Project.allMoveFilesForContentRoots(): List<MvFile> {
    val files = mutableListOf<MvFile>()
    processAllMvFilesOnce(
        (this.moveProjectsService as MoveProjectsServiceImpl).projects.currentState
    ) { file, _ ->
        val moveFile = file.toMvFile(this) ?: return@processAllMvFilesOnce
        files.add(moveFile)
    }
    return files
}
