package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MvFile
import org.move.lang.moveProject
import org.move.lang.toMvFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.*
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

data class DeclaredAddresses(
    val values: AddressMap,
    val placeholders: PlaceholderMap,
) {
    fun placeholdersAsValues(): AddressMap {
        val values = mutableAddressMap()
        for (ph in placeholders) {
            val value = ph.value.value?.stringValue() ?: continue
            values[ph.key] = AddressVal(value, ph.value, ph.value)
        }
        return values
    }

    fun get(name: String): AddressVal? {
        if (name in this.values) return this.values[name]
        if (name in this.placeholders) {
            val placeholderKeyVal = this.placeholders[name]
            return AddressVal(MvConstants.ADDR_PLACEHOLDER, null, placeholderKeyVal)
        }
        return null
    }
}

fun testEmptyMvProject(project: Project): MoveProject {
    val moveToml = MoveToml(project)
    val rootFile = project.contentRoots.first()
    val addresses = DeclaredAddresses(mutableAddressMap(), placeholderMap())
    return MoveProject(project, moveToml, rootFile, addresses, addresses.copy())
}

data class MoveProject(
    val project: Project,
    val moveToml: MoveToml,
    val root: VirtualFile,
    val declaredAddresses: DeclaredAddresses,
    val declaredDevAddresses: DeclaredAddresses
) {
    val packageName: String? get() = moveToml.packageTable?.name
    val rootPath: Path get() = root.toNioPath()

    fun projectDirPath(name: String): Path = rootPath.resolve(name)

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
        val addressVal = getAddresses()[addressName] ?: return null
        return addressVal.placeholderKeyValue?.singleSegmentOrNull()
            ?: addressVal.keyValue?.singleSegmentOrNull()
    }

    fun getAddressValue(name: String): String? {
        val addressVal = getAddresses()[name] ?: return null
        return addressVal.value
    }

    fun getAddresses(): AddressMap {
        // go through every dependency, extract
        // 1. MoveProject for that
        // 2. Substitution mapping for the dependency
        val addresses = mutableAddressMap()
        for (dep in this.moveToml.dependencies.values) {
            val moveTomlFile = dep.absoluteLocalPath.resolve("Move.toml")
                .findVirtualFile()
                ?.toPsiFile(this.project) ?: continue
            val depDeclaredAddrs = moveTomlFile.moveProject?.declaredAddresses ?: continue

            // apply substitutions
            val newPlaceholders = placeholderMap()
            val newAddresses = depDeclaredAddrs.values.copyMap()

            for ((placeholderName, placeholderKeyValue) in depDeclaredAddrs.placeholders.entries) {
                val placeholderSubst = dep.subst[placeholderName]
                if (placeholderSubst == null) {
                    newPlaceholders[placeholderName] = placeholderKeyValue
                    continue
                }
                val (value, keyValue) = placeholderSubst
                newAddresses[placeholderName] = AddressVal(value, keyValue, placeholderKeyValue)
            }
            // renames
            for ((renamedName, originalVal) in dep.subst.entries) {
                val (originalName, keyVal) = originalVal
                val addressVal = depDeclaredAddrs.get(originalName)
                if (addressVal != null) {
                    addresses[renamedName] =
                        AddressVal(addressVal.value, keyVal, null)
                }
            }
            addresses.putAll(newAddresses)
        }
        addresses.putAll(this.declaredAddresses.values)
        addresses.putAll(this.declaredAddresses.placeholdersAsValues())
        addresses.putAll(this.declaredDevAddresses.values)
        return addresses
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
