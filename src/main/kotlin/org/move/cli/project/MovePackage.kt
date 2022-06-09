package org.move.cli.project

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.*
import org.move.lang.toNioPathOrNull
import org.move.openapiext.checkReadAccessAllowed
import org.move.openapiext.resolveExisting
import org.toml.lang.psi.TomlFile
import java.nio.file.Path

data class MovePackage(
    private val project: Project,
    val contentRoot: VirtualFile,
    val moveToml: MoveToml,
) {
    val packageName = this.moveToml.packageName

    fun layout(): List<Path> {
        val rootPath = contentRoot.toNioPathOrNull() ?: return emptyList()
        val names = listOf(
            *MvProjectLayout.sourcesDirs,
            MvProjectLayout.testsDir,
            MvProjectLayout.buildDir
        )
        return names.mapNotNull { rootPath.resolveExisting(it) }
    }

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

    private fun setupContentRoots(
        project: Project,
        setup: ContentEntry.(ModifiableRootModel, VirtualFile) -> Unit
    ) {
        val packageModule = ModuleUtilCore.findModuleForFile(this.contentRoot, project) ?: return
        ModuleRootModificationUtil.updateModel(packageModule) { rootModel ->
            rootModel.contentEntries.singleOrNull()?.setup(rootModel, this.contentRoot)
        }
    }

    private fun iterateMovePackages() {
        // depth-first iteration over dependencies' MovePackage objects
    }

    companion object {
        fun fromMoveToml(moveToml: MoveToml): MovePackage? {
            checkReadAccessAllowed()
            val contentRoot = moveToml.tomlFile?.parent?.virtualFile ?: return null
            return MovePackage(moveToml.project, contentRoot, moveToml)
        }

        fun fromTomlFile(tomlFile: TomlFile): MovePackage? {
            checkReadAccessAllowed()
            val contentRoot = tomlFile.parent?.virtualFile ?: return null
            val contentRootPath = contentRoot.toNioPathOrNull() ?: return null
            val moveToml = MoveToml.fromTomlFile(tomlFile, contentRootPath)
            return MovePackage(tomlFile.project, contentRoot, moveToml)
        }
    }
}
