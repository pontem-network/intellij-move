package org.move.openapiext

import com.intellij.openapi.project.Project
import org.move.cli.processAllMoveFilesOnce
import org.move.cli.moveProjects
import org.move.lang.MoveFile

fun Project.allMoveFilesForContentRoots(): List<MoveFile> {
    val files = mutableListOf<MoveFile>()
    processAllMoveFilesOnce(this.moveProjects.allProjects) { moveFile, _ -> files.add(moveFile) }
    return files
}
