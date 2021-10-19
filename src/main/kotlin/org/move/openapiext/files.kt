package org.move.openapiext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.containers.addIfNotNull
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.move.cli.Constants
import org.move.cli.metadata
import org.move.manifest.DoveToml
import org.move.utils.rootService
import java.nio.file.Files
import java.nio.file.Path

fun Project.currentProjectFolders(currentFilePath: Path): List<VirtualFile> {
    // fetch metadata, and end processing if not available
    val metadata = this.metadata(currentFilePath) ?: return emptyList()

    val moduleFolders = mutableListOf<Path>()
    if (metadata.packageTable != null) {
        moduleFolders.addAll(metadata.packageTable.dependencies)
    }
    if (metadata.layoutTable?.modules_dir != null) {
        moduleFolders.add(metadata.layoutTable.modules_dir)
    }
    return moduleFolders.mapNotNull {
        VirtualFileManager.getInstance().findFileByNioPath(it)
    }
}

fun Project.allProjectsFolders(): List<VirtualFile> {
    // fetch metadata, and end processing if not available
    val moduleFolders = mutableListOf<Path>()
    for (filePath in Files.walk(this.rootService.path)) {
        if (filePath.isDirectory() && filePath.resolve(Constants.DOVE_MANIFEST_FILE).exists()) {
            val doveToml = DoveToml.parse(this, filePath.parent)
            val dependencies = doveToml?.packageTable?.dependencies.orEmpty()
            moduleFolders.addAll(dependencies)

            val modulesDir = doveToml?.layoutTable?.modules_dir
            moduleFolders.addIfNotNull(modulesDir)
        }
    }
    return moduleFolders.mapNotNull {
        VirtualFileManager.getInstance().findFileByNioPath(it)
    }
}
