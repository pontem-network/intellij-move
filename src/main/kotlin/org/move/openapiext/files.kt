package org.move.openapiext

import com.intellij.openapi.project.Project
import org.move.cli.MoveProjectsServiceImpl
import org.move.cli.moveProjects
import org.move.cli.processAllMvFilesOnce
import org.move.lang.MvFile
import org.move.lang.toMvFile

fun Project.allMoveFilesForContentRoots(): List<MvFile> {
    val files = mutableListOf<MvFile>()
    processAllMvFilesOnce(
        (this.moveProjects as MoveProjectsServiceImpl).projects.currentState
    ) { file, _ ->
        val mvFile = file.toMvFile(this) ?: return@processAllMvFilesOnce
        files.add(mvFile)
    }
    return files
}
