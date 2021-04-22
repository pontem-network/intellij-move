package org.move.cli

import com.google.gson.annotations.SerializedName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.move.settings.getDoveExecutable
import org.move.utils.rootService

data class GitDependency(
    val git: String,
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
    val module_dir: String,
    val script_dir: String,
    val tests_dir: String,
    val module_output: String,
    val script_output: String,
    val target_deps: String,
    val target: String,
    val index: String,
)

data class DoveProjectMetadata(
    @SerializedName("package")
    val package_info: PackageInfo,
    val layout: LayoutInfo,
)


@Service(Service.Level.PROJECT)
class MetadataService(private val project: Project) {
    var metadata: DoveProjectMetadata? = null
        private set

    init {
        this.refresh()
    }

    fun refresh() {
        // clean previous state
        this.metadata = null;

        val root = project.rootService.path ?: return
        val executable = project.getDoveExecutable() ?: return
        this.metadata = executable.metadata(root)
    }
}

val Project.metadataService: MetadataService
    get() = ServiceManager.getService(
        this,
        MetadataService::class.java
    )
