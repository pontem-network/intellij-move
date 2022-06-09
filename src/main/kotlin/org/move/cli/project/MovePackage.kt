package org.move.cli.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.*
import org.move.lang.toNioPathOrNull
import org.move.openapiext.checkReadAccessAllowed
import org.toml.lang.psi.TomlFile

data class MovePackage(
    private val project: Project,
    val contentRoot: VirtualFile,
    val moveToml: MoveToml,
) {
    val packageName = this.moveToml.packageName

    fun declaredAddresses(): DeclaredAddresses {
        val tomlMainAddresses = moveToml.declaredAddresses(DevMode.MAIN)
        val tomlDevAddresses = moveToml.declaredAddresses(DevMode.DEV)

        val addresses = mutableAddressMap()
        addresses.putAll(tomlMainAddresses.values)
        // add placeholders defined in this package as address values
        addresses.putAll(tomlMainAddresses.placeholdersAsValues())
        // devs on top
        addresses.putAll(tomlDevAddresses.values)

        return DeclaredAddresses(addresses, tomlMainAddresses.placeholders)
    }

    fun dependencyAddresses(): Pair<AddressMap, PlaceholderMap> {
        val addrs = mutableAddressMap()
        val placeholders = placeholderMap()
        for ((dep, subst) in moveToml.dependencies.values) {
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
            addrs.putAll(newDepAddresses)
            placeholders.putAll(newDepPlaceholders)
        }
        return addrs to placeholders
    }

    companion object {
        fun fromTomlFile(tomlFile: TomlFile): MovePackage? {
            checkReadAccessAllowed()
            val contentRoot = tomlFile.parent?.virtualFile ?: return null
            val contentRootPath = contentRoot.toNioPathOrNull() ?: return null
            val moveToml = MoveToml.fromTomlFile(tomlFile, contentRootPath)
            return MovePackage(tomlFile.project, contentRoot, moveToml)
        }
    }
}
