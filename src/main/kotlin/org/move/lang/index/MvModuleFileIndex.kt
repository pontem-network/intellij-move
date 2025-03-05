package org.move.lang.index

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import org.move.cli.MoveProject
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvModule
import org.move.lang.core.types.Address
import org.move.lang.core.types.Address.Named
import org.move.lang.core.types.address
import org.move.lang.core.types.fqName
import org.move.lang.toMoveFile

class MvModuleFileIndex: MvScalarFileIndexExtension() {
    override fun getName(): ID<String, Void> = INDEX_ID
    override fun getVersion(): Int = 2

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return object: DataIndexer<String, Void?, FileContent> {
            override fun map(inputData: FileContent): Map<String, Void?> {
                val file = inputData.psiFile as MoveFile
                // build a list of all modules for which there is module specs
                return buildMap {
                    for (module in file.modules()) {
                        val fqName = module.fqName() ?: continue
                        set(fqName.indexId(), null)
                    }
                }
            }
        }
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        val INDEX_ID: ID<String, Void> = ID.create("org.move.index.MvModuleFileIndex")

        fun getAllModulesForCompletion(
            moveProject: MoveProject,
            searchScope: GlobalSearchScope,
            address: Address
        ): List<MvModule> {
            val filesIndex = FileBasedIndex.getInstance()
            val project = moveProject.project
            val allModuleIds = filesIndex.getAllKeys(INDEX_ID, project)

            val prefixes = address.searchIndexIds(moveProject).map { "$it::" }
            val prefixedIds = allModuleIds.filter { id -> prefixes.any { id.startsWith(it) } }.toHashSet()

            return buildList {
                val vFiles =
                    filesIndex.getContainingFilesForAnyKey(INDEX_ID, prefixedIds, searchScope)
                val files = vFiles.mapNotNull { it.toMoveFile(project) }
                for (file in files) {
                    val filtered = file.modules().filterByAddress(moveProject, address, isCompletion = true)
                    addAll(filtered)
                }
            }
        }

        fun getModulesForId(moveProject: MoveProject, address: Address, moduleName: String): List<MvModule> {
            val project = moveProject.project
            val searchScope = moveProject.searchScope()
            val filesIndex = FileBasedIndex.getInstance()
            val searchIndexIds = address.searchIndexIds(moveProject).map { "$it::$moduleName" }.toHashSet()
            return buildList {
                val vFiles =
                    filesIndex.getContainingFilesForAnyKey(INDEX_ID, searchIndexIds, searchScope)
                val files = vFiles.mapNotNull { it.toMoveFile(project) }
                for (file in files) {
                    val filtered =
                        file.modules().filterByAddress(moveProject, address, isCompletion = false)
                    addAll(filtered)
                }
            }
        }
    }
}

private fun List<MvModule>.filterByAddress(
    moveProject: MoveProject,
    address: Address,
    isCompletion: Boolean
): List<MvModule> {
    // if no Aptos project, then cannot match by address
    return this.filter { it.matchesByAddress(moveProject, address, isCompletion) }
}

private fun MvModule.matchesByAddress(moveProject: MoveProject, address: Address, isCompletion: Boolean): Boolean {
    val moduleAddress = this.address() ?: return false
    val sameValues = Address.equals(moduleAddress, address, moveProject)

    if (sameValues && isCompletion) {
        // compare named addresses by name in case of the same values for the completion
        if (address is Named && moduleAddress is Named) {
            return address.name == moduleAddress.name
        }
    }

    return sameValues
}
