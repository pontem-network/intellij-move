package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.moveProject
import org.move.openapiext.findVirtualFile
import org.move.openapiext.resolveExisting
import org.move.openapiext.toPsiFile
import java.nio.file.Path

data class ProjectInfo(
    val rootPath: Path,
    val dependencies: DependenciesMap,
    val dev_dependencies: DependenciesMap,
) {
    val sourcesFolder: VirtualFile? get() = rootPath.resolveExisting("sources")?.findVirtualFile()

    fun deps(scope: GlobalScope): DependenciesMap {
        return when (scope) {
            GlobalScope.MAIN -> this.dependencies
            GlobalScope.DEV -> (this.dependencies + this.dev_dependencies) as DependenciesMap
        }
    }
}

val MoveProject.projectInfo: ProjectInfo?
    get() {
        val rootPath = this.rootPath ?: return null
        return ProjectInfo(
            rootPath,
            this.moveToml.dependencies.asDependenciesMap(),
            this.moveToml.dev_dependencies.asDependenciesMap(),
        )
    }


sealed class Dependency {
    abstract fun projectInfo(project: Project): ProjectInfo?
    abstract fun declaredAddresses(project: Project): DeclaredAddresses?

    data class Local(val absoluteLocalPath: Path) : Dependency() {
        override fun declaredAddresses(project: Project): DeclaredAddresses? {
            val moveTomlFile = this.absoluteLocalPath.resolve("Move.toml")
                .findVirtualFile()
                ?.toPsiFile(project) ?: return null
            return moveTomlFile.moveProject?.declaredAddresses() ?: return null
        }

        override fun projectInfo(project: Project): ProjectInfo? {
            val moveTomlPath = absoluteLocalPath.resolve("Move.toml")
            val p = project.moveProjects.findProjectForPath(moveTomlPath) ?: return null
            return p.projectInfo
        }
    }

    data class Git(val dirPath: Path) : Dependency() {
        override fun projectInfo(project: Project): ProjectInfo? {
            val buildInfo = BuildInfo.fromRootPath(dirPath) ?: return null
            val buildDir = dirPath.parent

            val dependencies = dependenciesMap()
            for (depName in buildInfo.dependencies) {
                val depDir = buildDir.resolveExisting(depName) ?: continue
                dependencies[depName] = Git(depDir)
            }
            return ProjectInfo(dirPath, dependencies, dependencies)
        }

        override fun declaredAddresses(project: Project): DeclaredAddresses? {
            val buildInfo = BuildInfo.fromRootPath(dirPath) ?: return null
            val addresses = buildInfo.addresses()
            return DeclaredAddresses(addresses, mutableMapOf())
        }
    }
}
