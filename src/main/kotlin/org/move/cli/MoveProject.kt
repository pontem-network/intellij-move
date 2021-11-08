package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MoveFile
import org.move.lang.toMoveFile
import org.move.openapiext.findKey
import org.move.openapiext.findVirtualFile
import org.move.openapiext.getTable
import org.move.openapiext.parseToml
import org.move.stdext.deepIterateChildrenRecursivery
import org.toml.lang.psi.TomlKeySegment
import java.nio.file.Path

enum class GlobalScope {
    MAIN, DEV;
}

data class MoveModuleFile(
    val file: MoveFile,
    val addressSubst: Map<String, String>,
)

data class MoveProject(
    val project: Project,
    val moveToml: MoveToml
) {
    fun getModuleFolders(scope: GlobalScope): List<VirtualFile> {
        val deps = when (scope) {
            GlobalScope.MAIN -> moveToml.dependencies
            GlobalScope.DEV -> moveToml.dependencies + moveToml.dev_dependencies
        }
        val folders = mutableListOf<VirtualFile>()
        val sourcesFolder = moveToml.root.resolve("sources").findVirtualFile()
        if (sourcesFolder != null) {
            folders.add(sourcesFolder)
        }
        for (dep in deps.values) {
            // TODO: make them absolute paths
            val folder = dep.local.resolve("sources").findVirtualFile() ?: continue
            if (folder.isDirectory)
                folders.add(folder)
        }
        return folders
    }

    fun getAddressTomlKeySegment(addressName: String): TomlKeySegment? {
        return this.moveToml.tomlFile.getTable("addresses")
            ?.findKey(addressName)
            ?.segments
            ?.singleOrNull()
    }

    fun processModuleFiles(scope: GlobalScope, processFile: (MoveModuleFile) -> Boolean) {
        val folders = getModuleFolders(scope)
        for (folder in folders) {
            deepIterateChildrenRecursivery(folder, { it.extension == "move" }) { file ->
                val moveFile = file.toMoveFile(project) ?: return@deepIterateChildrenRecursivery true
                val moduleFile = MoveModuleFile(moveFile, emptyMap())
                processFile(moduleFile)
            }
        }
    }

    companion object {
        fun fromMoveTomlPath(project: Project, moveTomlPath: Path): MoveProject? {
            val tomlFile = parseToml(project, moveTomlPath) ?: return null
            val moveToml = MoveToml.fromTomlFile(tomlFile) ?: return null
            return MoveProject(project, moveToml)
        }
    }
}
