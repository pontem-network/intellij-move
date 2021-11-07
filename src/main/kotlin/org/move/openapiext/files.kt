package org.move.openapiext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.move.cli.MoveConstants
import org.move.cli.GlobalScope
import org.move.cli.MoveToml
import org.move.lang.MoveFile
import org.move.utils.getMoveFilesInFolder
import org.move.utils.rootService
import java.nio.file.Files

fun Project.allProjectMoveFiles(): List<MoveFile> {
    return this.allProjectsFolders()
        .flatMap { getMoveFilesInFolder(this, it) }
}

fun Project.allProjectsFolders(): List<VirtualFile> {
    val moduleFolders = mutableListOf<VirtualFile>()
    for (filePath in Files.walk(this.rootService.path)) {
        if (filePath.isDirectory()
            && filePath.resolve(MoveConstants.MANIFEST_FILE).exists()
        ) {
            val moveToml =
                parseToml(this, filePath)?.let { MoveToml.fromTomlFile(it) } ?: continue
            val folders = moveToml.getFolders(GlobalScope.DEV)
            moduleFolders.addAll(folders)
        }
    }
    return moduleFolders
}
