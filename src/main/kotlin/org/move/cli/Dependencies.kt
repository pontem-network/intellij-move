package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.project.BuildInfoYaml
import org.move.lang.moveProject
import org.move.openapiext.toVirtualFile
import org.move.openapiext.resolveExisting
import org.move.openapiext.toPsiFile
import java.nio.file.Path

data class ProjectInfo(
    val rootPath: Path,
    val dependencies: DependenciesMap,
    val dev_dependencies: DependenciesMap,
) {
    val sourcesFolder: VirtualFile? get() = rootPath.resolveExisting("sources")?.toVirtualFile()

    fun deps(devMode: DevMode): DependenciesMap {
        return when (devMode) {
            DevMode.MAIN -> this.dependencies
            DevMode.DEV -> (this.dependencies + this.dev_dependencies) as DependenciesMap
        }
    }
}

val MoveProject.projectInfo: ProjectInfo?
    get() {
        val rootPath = this.contentRootPath ?: return null
        return ProjectInfo(
            rootPath,
            this.currentPackage.moveToml.dependencies.asDependenciesMap(),
            this.currentPackage.moveToml.dev_dependencies.asDependenciesMap(),
        )
    }


sealed class Dependency {
    abstract fun projectInfo(project: Project): ProjectInfo?
    abstract fun declaredAddresses(project: Project): DeclaredAddresses?

    data class Local(val absoluteLocalPath: Path) : Dependency() {
        override fun declaredAddresses(project: Project): DeclaredAddresses? {
            val moveTomlFile = this.absoluteLocalPath.resolve("Move.toml")
                .toVirtualFile()
                ?.toPsiFile(project) ?: return null
            return moveTomlFile.moveProject?.declaredAddresses() ?: return null
        }

        override fun projectInfo(project: Project): ProjectInfo? {
            val moveTomlPath = absoluteLocalPath.resolve("Move.toml")
            val p = project.projectsService.findProjectForPath(moveTomlPath) ?: return null
            return p.projectInfo
        }
    }

    data class Git(val dirPath: Path) : Dependency() {
        override fun projectInfo(project: Project): ProjectInfo? {
            val yamlPath = dirPath.resolveExisting("BuildInfo.yaml") ?: return null
            val buildInfoYaml = BuildInfoYaml.fromPath(yamlPath) ?: return null

            val buildDir = dirPath.parent
            val dependencies = dependenciesMap()
            for (depName in buildInfoYaml.dependencies) {
                val depDir = buildDir.resolveExisting(depName) ?: continue
                dependencies[depName] = Git(depDir)
            }
            return ProjectInfo(dirPath, dependencies, dependencies)
        }

        override fun declaredAddresses(project: Project): DeclaredAddresses? {
            val yamlPath = dirPath.resolveExisting("BuildInfo.yaml") ?: return null
            val buildInfoYaml = BuildInfoYaml.fromPath(yamlPath) ?: return null

            val addresses = buildInfoYaml.addresses()
            return DeclaredAddresses(addresses, mutableMapOf())
        }
    }
}
