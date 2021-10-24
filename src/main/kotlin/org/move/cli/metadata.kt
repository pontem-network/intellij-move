package org.move.cli

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MoveFile
import org.move.manifest.AddressesMap
import org.move.manifest.DoveToml
import org.move.manifest.MoveToml
import org.move.openapiext.resolveAbsPath
import org.move.utils.iterateMoveFilesInFolder
import java.nio.file.Path


enum class ManifestType {
    MOVE_CLI, DOVE;
}

enum class NamedAddressScope {
    MAIN, DEV;
}

data class MoveModuleFile(
    val file: MoveFile,
    val addressSubst: Map<String, String>,
)

data class ProjectMetadata(
    val dialect: String?,
    val account_address: String?,
    val moduleFolders: List<VirtualFile>,
//    val depModules: List<VirtualFile>,
    val namedAddresses: Map<NamedAddressScope, AddressesMap> = mutableMapOf(
        NamedAddressScope.MAIN to emptyMap(),
        NamedAddressScope.DEV to emptyMap()
    ),
    val project: Project,
) {
    /// processFile should return false if iteration should be stopped
    fun iterOverMoveModuleFiles(processFile: (MoveModuleFile) -> Boolean) {
        for (folder in moduleFolders) {
            iterateMoveFilesInFolder(project, folder) {
                val moduleFile = MoveModuleFile(it, emptyMap())
                return@iterateMoveFilesInFolder !processFile(moduleFile)
            }
        }
    }
}

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
                ProjectMetadata(
                    doveToml?.packageTable?.dialect,
                    doveToml?.packageTable?.account_address,
                    doveToml?.getFolders().orEmpty(),
                    project = project,
                )
            }
            ManifestType.MOVE_CLI -> {
                val moveToml = MoveToml.parse(this.project, manifestPath.parent)
                val namedAddresses = mutableMapOf<NamedAddressScope, AddressesMap>()
                if (moveToml != null) {
                    namedAddresses[NamedAddressScope.MAIN] = moveToml.addresses
                    namedAddresses[NamedAddressScope.DEV] = moveToml.dev_addresses
                }
                ProjectMetadata("diem", null, emptyList(), namedAddresses, project)
            }
        }
    }
}

fun Project.metadata(currentFilePath: Path): ProjectMetadata? {
    val metadataService = this.getService(MetadataService::class.java)
    return metadataService.metadata(currentFilePath)
}
