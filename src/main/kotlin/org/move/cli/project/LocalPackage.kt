package org.move.cli.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.*

data class LocalPackage(
    override val contentRoot: VirtualFile,
    private val project: Project,
    val moveToml: MoveToml,
): Package(contentRoot) {

    val packageName = this.moveToml.packageName

    override fun addresses(): DeclaredAddresses {
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
}
