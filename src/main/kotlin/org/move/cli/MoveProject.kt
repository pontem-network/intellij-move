package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.MvFile
import org.move.lang.core.types.Address
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
        for ((name, pVal) in placeholders.entries) {
            val value = pVal.keyValue.value?.stringValue() ?: continue
            values[name] = AddressVal(value, pVal.keyValue, pVal.keyValue, pVal.packageName)
        }
        return values
    }

    fun get(name: String): AddressVal? {
        if (name in this.values) return this.values[name]
        return this.placeholders[name]
            ?.let {
                AddressVal(MvConstants.ADDR_PLACEHOLDER, null, it.keyValue, it.packageName)
            }
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
    subst: RawAddressMap,
    packageName: String
): Pair<AddressMap, PlaceholderMap> {
    val newDepAddresses = addresses.values.copyMap()
    val newDepPlaceholders = placeholderMap()

    for ((pName, pVal) in addresses.placeholders.entries) {
        val placeholderSubst = subst[pName]
        if (placeholderSubst == null) {
            newDepPlaceholders[pName] = pVal
            continue
        }
        val (value, keyValue) = placeholderSubst
        newDepAddresses[pName] = AddressVal(value, keyValue, pVal.keyValue, packageName)
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
    val packageName: String? get() = moveToml.packageName

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

            val (newDepAddresses, newDepPlaceholders) = applyAddressSubstitutions(
                depDeclaredAddrs,
                subst,
                packageName ?: ""
            )

            // renames
            for ((renamedName, originalVal) in subst.entries) {
                val (originalName, keyVal) = originalVal
                val origVal = depDeclaredAddrs.get(originalName)
                if (origVal != null) {
                    newDepAddresses[renamedName] =
                        AddressVal(origVal.value, keyVal, null, packageName ?: "")
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
        val address = Address(addressVal.value)
        return address.normalized().shortText()
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
