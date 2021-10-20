package org.move.cli

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.manifest.DoveToml
import org.move.manifest.MoveToml
import org.move.openapiext.resolveAbsPath
import java.nio.file.Path


enum class ManifestType {
    MOVE_CLI, DOVE;
}

data class ProjectMetadata(
    val dialect: String?,
    val account_address: String?,
    val depFolders: List<VirtualFile>,
)

@Service(Service.Level.PROJECT)
class MetadataService(private val project: Project) {
    private fun findCurrentTomlManifest(currentFilePath: Path): Pair<Path, ManifestType>? {
        var dir = currentFilePath.parent
        while (dir != null) {
            val doveTomlPath = dir.resolveAbsPath(Constants.DOVE_MANIFEST_FILE)
            if (doveTomlPath != null) {
                return Pair(doveTomlPath, ManifestType.DOVE)
            }
            val moveTomlPath = dir.resolveAbsPath(Constants.MOVE_CLI_MANIFEST_FILE)
            if (moveTomlPath != null) {
                return Pair(moveTomlPath, ManifestType.MOVE_CLI)
            }
            dir = dir.parent
        }
        return null
    }

    fun metadata(currentFilePath: Path): ProjectMetadata? {
        val (manifestPath, manifestType) =
            findCurrentTomlManifest(currentFilePath) ?: return null
        return when (manifestType) {
            ManifestType.DOVE -> {
                val doveToml = DoveToml.parse(this.project, manifestPath.parent)
                ProjectMetadata(doveToml?.packageTable?.dialect,
                                doveToml?.packageTable?.account_address,
                                doveToml?.getFolders().orEmpty())
            }
            ManifestType.MOVE_CLI -> {
//                val moveToml = MoveToml.parse(this.project, manifestPath.parent)
                ProjectMetadata("diem", null, emptyList())
            }
        }
    }
}

fun Project.metadata(currentFilePath: Path): ProjectMetadata? {
    val metadataService = this.getService(MetadataService::class.java)
    return metadataService.metadata(currentFilePath)
}
