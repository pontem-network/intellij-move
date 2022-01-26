package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MvFile
import org.move.lang.toMvFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.contentRoots
import org.move.openapiext.stringValue
import org.move.stdext.deepIterateChildrenRecursivery
import java.nio.file.Path
import java.util.*

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

fun applyAddressSubstitutions(
    addresses: DeclaredAddresses,
    subst: RawAddressMap
): Pair<AddressMap, PlaceholderMap> {
    val newDepAddresses = addresses.values.copyMap()
    val newDepPlaceholders = placeholderMap()

    for ((placeholderName, placeholderKeyValue) in addresses.placeholders.entries) {
        val placeholderSubst = subst[placeholderName]
        if (placeholderSubst == null) {
            newDepPlaceholders[placeholderName] = placeholderKeyValue
            continue
        }
        val (value, keyValue) = placeholderSubst
        newDepAddresses[placeholderName] = AddressVal(value, keyValue, placeholderKeyValue)
    }
    return Pair(newDepAddresses, newDepPlaceholders)
}

data class MoveProject(
    val project: Project,
    val moveToml: MoveToml,
    val root: VirtualFile,
    val declaredAddrs: DeclaredAddresses,
    val declaredDevAddresses: DeclaredAddresses
) {
    val packageName: String? get() = moveToml.packageTable?.name

    val rootPath: Path? get() = root.toNioPathOrNull()
    fun projectDirPath(name: String): Path? = rootPath?.resolve(name)

    fun moduleFolders(scope: GlobalScope): List<VirtualFile> {
        val q = ArrayDeque<ProjectInfo>()
        val folders = mutableListOf<VirtualFile>()
        val projectInfo = this.projectInfo ?: return emptyList()
        val s = projectInfo.sourcesFolder
        if (s != null) {
            folders.add(s)
        }
        q.add(projectInfo)
        while (q.isNotEmpty()) {
            val info = q.pop()
            val depInfos = info.deps(scope).values.mapNotNull { it.projectInfo(project) }
            q.addAll(depInfos)
            folders.addAll(depInfos.mapNotNull { it.sourcesFolder })
        }
        return folders
    }

    fun declaredAddresses(): DeclaredAddresses {
        val addresses = mutableAddressMap()
        val placeholders = placeholderMap()
        for ((dep, subst) in this.moveToml.dependencies.values) {
            val depDeclaredAddrs = dep.declaredAddresses(project) ?: continue

            val (newDepAddresses, newDepPlaceholders) = applyAddressSubstitutions(depDeclaredAddrs, subst)

            // renames
            for ((renamedName, originalVal) in subst.entries) {
                val (originalName, keyVal) = originalVal
                val addressVal = depDeclaredAddrs.get(originalName)
                if (addressVal != null) {
                    newDepAddresses[renamedName] =
                        AddressVal(addressVal.value, keyVal, null)
                }
            }
            addresses.putAll(newDepAddresses)
            placeholders.putAll(newDepPlaceholders)
        }
        // add addresses defined in this package
        addresses.putAll(this.declaredAddrs.values)
        // add placeholders defined in this package as address values
        addresses.putAll(this.declaredAddrs.placeholdersAsValues())
        // add dev-addresses
        addresses.putAll(this.declaredDevAddresses.values)

        // add placeholders that weren't filled
        placeholders.putAll(this.declaredAddrs.placeholders)

        return DeclaredAddresses(addresses, placeholders)
    }

    fun addresses(): AddressMap {
        return declaredAddresses().values
    }

    fun getAddressValue(name: String): String? {
        val addressVal = addresses()[name] ?: return null
        return addressVal.value
    }

    fun processModuleFiles(scope: GlobalScope, processFile: (MvModuleFile) -> Boolean) {
        val folders = moduleFolders(scope)
        for (folder in folders) {
            deepIterateChildrenRecursivery(folder, { it.extension == "move" }) { file ->
                val moveFile = file.toMvFile(project) ?: return@deepIterateChildrenRecursivery true
                val moduleFile = MvModuleFile(moveFile, emptyMap())
                processFile(moduleFile)
            }
        }
    }
}
