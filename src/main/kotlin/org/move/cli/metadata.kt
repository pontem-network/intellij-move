package org.move.cli

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.move.manifest.DoveToml
import org.move.openapiext.resolveAbsPath
import java.nio.file.Path


@Service(Service.Level.PROJECT)
class MetadataService(private val project: Project) {
    private fun findDoveToml(currentFilePath: Path): Path? {
        var dir = currentFilePath.parent
        while (dir != null) {
            val doveTomlPath = dir.resolveAbsPath(Constants.DOVE_MANIFEST_FILE)
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
