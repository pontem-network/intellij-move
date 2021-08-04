package org.move.cli

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.move.openapiext.resolveAbsPath
import org.move.manifest.dovetoml.DoveToml
import java.nio.file.Path

data class GitDependency(
    val git: String,
    val branch: String?,
    val rev: String?,
    val tag: String?,
    val path: String?,
    val local_paths: List<String>
)

data class PackageInfo(
    val name: String,
    val account_address: String,
    val authors: List<String>,
    val local_dependencies: List<String>,
    val git_dependencies: List<GitDependency>,
    val blockchain_api: String?,
    val dialect: String,
)

data class LayoutInfo(
    val modules_dir: String,
    val scripts_dir: String,
    val tests_dir: String,
    val modules_output: String,
    val scripts_output: String,
    val transactions_output: String,
    val bundles_output: String,
    val deps: String,
    val artifacts: String,
    val index: String,
)

//data class DoveProjectMetadata(
//    @SerializedName("package")
//    val package_info: PackageInfo,
//    val layout: LayoutInfo,
//)


@Service(Service.Level.PROJECT)
class MetadataService(private val project: Project) {
    private fun findDoveToml(currentFilePath: Path): Path? {
        var dir = currentFilePath.parent
        while (dir != null) {
            val doveTomlPath = dir.resolveAbsPath("Dove.toml")
            if (doveTomlPath != null) {
                return doveTomlPath
            }
            dir = dir.parent
        }
        return null
    }

    fun metadata(currentFilePath: Path): DoveToml? {
        val doveTomlPath = findDoveToml(currentFilePath) ?: return null
        return DoveToml.parse(this.project, doveTomlPath.parent)
    }

//    var metadata: DoveToml? = null
//        private set
//
//    init {
//        this.refresh()
//    }

//    fun refresh() {
        // clean previous state
//        this.doveToml = null
//        this.metadata =
//            project.rootService.path?.let { DoveToml.parse(project, it) }
//        val root = project.rootService.path ?: return
//        this.doveToml = DoveToml.parse(project, root)
//        val executable = project.getDoveExecutable() ?: return
//        this.doveToml = executable.metadata(root)
//    }
}

fun Project.metadata(currentFilePath: Path): DoveToml? {
    val metadataService = this.getService(MetadataService::class.java)
    return metadataService.metadata(currentFilePath)
}
