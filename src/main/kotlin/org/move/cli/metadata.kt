package org.move.cli

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MoveFile
import org.move.manifest.AddressesMap
import org.move.manifest.DoveToml
import org.move.manifest.MoveToml
import org.move.openapiext.parseToml
import org.move.openapiext.resolveAbsPath
import org.move.utils.iterateMoveFilesInFolder
import java.nio.file.Path


enum class ManifestType {
    MOVE_CLI, DOVE;
}

enum class GlobalScope {
    MAIN, DEV;
}

data class MoveModuleFile(
    val file: MoveFile,
    val addressSubst: Map<String, String>,
)

data class ProjectMetadata(
    val project: Project,
    val manifestType: ManifestType,
    val dialect: String?,

    val moduleFolders: List<VirtualFile>,
    val devModuleFolders: List<VirtualFile>,

    val addresses: AddressesMap,
    val devAddresses: AddressesMap,
) {
    /// processFile should return false if iteration should be stopped
    fun iterOverMoveModuleFiles(processFile: (MoveModuleFile) -> Boolean) {
        for (folder in moduleFolders) {
            iterateMoveFilesInFolder(project, folder) {
                val moduleFile = MoveModuleFile(it, emptyMap())
                processFile(moduleFile)
            }
        }
    }
}

fun findCurrentTomlManifest(currentFilePath: Path): Pair<Path, ManifestType>? {
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

@Service(Service.Level.PROJECT)
class MetadataService(private val project: Project) {
    fun metadata(currentFilePath: Path): ProjectMetadata? {
        val (manifestPath, manifestType) =
            findCurrentTomlManifest(currentFilePath) ?: return null
        return when (manifestType) {
            ManifestType.DOVE -> {
                val doveToml = DoveToml.parse(this.project, manifestPath.parent)
                ProjectMetadata(
                    project,
                    manifestType,
                    doveToml?.packageTable?.dialect,
                    doveToml?.getFolders().orEmpty(),
                    emptyList(),
                    emptyMap(),
                    emptyMap(),
                )
            }
            ManifestType.MOVE_CLI -> {
                val tomlFile = parseToml(this.project, manifestPath) ?: return null
                val moveToml = MoveToml.parse(tomlFile)
                ProjectMetadata(
                    project,
                    manifestType,
                    "diem",
                    moveToml?.getFolders(GlobalScope.MAIN).orEmpty(),
                    moveToml?.getFolders(GlobalScope.DEV).orEmpty(),
                    moveToml?.addresses.orEmpty(),
                    moveToml?.dev_addresses.orEmpty()
                )
            }
        }
    }
}

fun Project.metadata(currentFilePath: Path): ProjectMetadata? {
    return this
        .getService(MetadataService::class.java)
        .metadata(currentFilePath)
}
