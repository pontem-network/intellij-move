package org.move.openapiext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.move.cli.metadataService
import java.nio.file.Path

fun Project.folders(): List<VirtualFile> {
    // fetch metadata, and end processing if not available
    val metadata = this.metadataService.metadata ?: return emptyList()

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
