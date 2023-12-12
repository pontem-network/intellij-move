package org.move.openapiext

import com.intellij.openapi.project.Project
import org.move.cli.fsDepth
import org.move.cli.moveProjectsService
import org.move.lang.MoveFile

fun Project.allMoveFiles(): List<MoveFile> {
    val files = mutableListOf<MoveFile>()
    val visited = mutableSetOf<String>()
    // find Move.toml files in all project roots, remembering depth of those
    // then search all .move children of those files, with biggest depth first
    this.moveProjectsService.allProjects
        .sortedByDescending { it.contentRoot.fsDepth }
        .map { moveProject ->
            moveProject.processMoveFiles {
                val filePath = it.virtualFile.path

                if (filePath in visited) return@processMoveFiles true
                visited.add(filePath)

                files.add(it)
                true
            }
        }
    return files
}
