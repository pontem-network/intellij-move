package org.move.openapiext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.move.cli.Constants
import org.move.manifest.DoveToml
import org.move.utils.rootService
import java.nio.file.Files

fun Project.allProjectsFolders(): List<VirtualFile> {
    val moduleFolders = mutableListOf<VirtualFile>()
    for (filePath in Files.walk(this.rootService.path)) {
        if (filePath.isDirectory() && filePath.resolve(Constants.DOVE_MANIFEST_FILE).exists()) {
            val doveToml = DoveToml.parse(this, filePath.parent)
            val folders = doveToml?.getFolders().orEmpty()
            moduleFolders.addAll(folders)
        }
    }
    return moduleFolders
}
