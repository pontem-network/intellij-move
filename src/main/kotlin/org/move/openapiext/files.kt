package org.move.openapiext

import com.intellij.openapi.project.Project
import org.move.cli.processAllMoveFilesOnce
import org.move.cli.projectsService
import org.move.lang.MoveFile

fun Project.allMoveFilesForContentRoots(): List<MoveFile> {
    val files = mutableListOf<MoveFile>()
    processAllMoveFilesOnce(this.projectsService.allProjects) { moveFile, _ -> files.add(moveFile) }
    return files
}
