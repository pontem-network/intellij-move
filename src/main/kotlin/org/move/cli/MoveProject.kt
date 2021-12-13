package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MvFile
import org.move.lang.moveProject
import org.move.lang.toMvFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.contentRoots
import org.move.openapiext.findVirtualFile
import org.move.openapiext.toPsiFile
import org.move.stdext.deepIterateChildrenRecursivery
import org.toml.lang.psi.TomlKeySegment
import java.nio.file.Path

enum class GlobalScope {
    MAIN, DEV;
}

data class MvModuleFile(
    val file: MvFile,
    val addressSubst: Map<String, String>,
)

typealias AddressesMap = Map<String, String>

data class DependencyAddresses(
    val values: AddressesMap,
    val placeholders: List<String>,
)

fun testEmptyMvProject(project: Project): MoveProject {
    val moveToml = MoveToml(
        project, null, null, emptyMap(), emptyMap(), sortedMapOf(), sortedMapOf()
    )
    val rootFile = project.contentRoots.first()
    val dependencies = DependencyAddresses(emptyMap(), emptyList())
    return MoveProject(project, moveToml, rootFile, dependencies)
}

data class MoveProject(
    val project: Project,
    val moveToml: MoveToml,
    val root: VirtualFile,
    val dependencyAddresses: DependencyAddresses,
) {
    val packageName: String? get() = moveToml.packageTable?.name
    val rootPath: Path get() = root.toNioPath()

    fun projectDirPath(name: String) = rootPath.resolve(name)

    fun getModuleFolders(scope: GlobalScope): List<VirtualFile> {
        // TODO: add support for git folders
        val deps = when (scope) {
            GlobalScope.MAIN -> moveToml.dependencies
            GlobalScope.DEV -> moveToml.dependencies + moveToml.dev_dependencies
        }
        val folders = mutableListOf<VirtualFile>()
        val sourcesFolder = root.toNioPathOrNull()?.resolve("sources")?.findVirtualFile()
        if (sourcesFolder != null) {
            folders.add(sourcesFolder)
        }
        for (dep in deps.values) {
            // TODO: make them absolute paths
            val folder = dep.absoluteLocalPath.resolve("sources").findVirtualFile() ?: continue
            if (folder.isDirectory)
                folders.add(folder)
        }
        return folders
    }

    fun getAddressTomlKeySegment(addressName: String): TomlKeySegment? {
        var resolved: TomlKeySegment? = null
        processNamedAddresses(this, emptyMap()) { segment, _ ->
            if (segment.name == addressName) {
                resolved = segment
                return@processNamedAddresses true
            }
            false
        }
        return resolved
    }

    fun getAddresses(): Map<String, String> {
        // go through every dependency, extract
        // 1. MvProject for that
        // 2. Substitution mapping for the dependency
        val values = mutableMapOf<String, String>()
        for (dep in this.moveToml.dependencies.values) {
            val moveTomlFile = dep.absoluteLocalPath.resolve("Move.toml")
                .findVirtualFile()
                ?.toPsiFile(this.project) ?: continue
            val depAddresses = moveTomlFile.moveProject?.dependencyAddresses ?: continue

            // apply substitutions
            val substitutions = dep.addrSubst
            val newPlaceholders = mutableListOf<String>()
            val newValues = depAddresses.values.toMutableMap()
            for (placeholder in depAddresses.placeholders) {
                val placeholderSubst = substitutions[placeholder]
                if (placeholderSubst == null) {
                    newPlaceholders.add(placeholder)
                    continue
                }
                newValues[placeholder] = placeholderSubst
            }
            values.putAll(newValues)
        }
        values.putAll(this.dependencyAddresses.values)
        return values
    }

    fun processModuleFiles(scope: GlobalScope, processFile: (MvModuleFile) -> Boolean) {
        val folders = getModuleFolders(scope)
        for (folder in folders) {
            deepIterateChildrenRecursivery(folder, { it.extension == "move" }) { file ->
                val moveFile = file.toMvFile(project) ?: return@deepIterateChildrenRecursivery true
                val moduleFile = MvModuleFile(moveFile, emptyMap())
                processFile(moduleFile)
            }
        }
    }
}
