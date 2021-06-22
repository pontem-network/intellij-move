package org.move.openapiext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.move.cli.metadataService
import java.nio.file.Paths

fun Project.folders(): List<VirtualFile> {
    // fetch metadata, and end processing if not available
    val metadata = this.metadataService.metadata ?: return emptyList()
    val moduleFolders = listOf(
        metadata.package_info.local_dependencies,
        listOf(metadata.layout.modules_dir),
        metadata.package_info.git_dependencies.flatMap { it.local_paths }
    ).flatten()
    return moduleFolders.mapNotNull {
        VirtualFileManager.getInstance().findFileByNioPath(Paths.get(it))
    }
}
