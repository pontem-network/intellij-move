package org.move.lang.index

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.move.cli.MoveProject
import org.move.lang.MoveFile
import org.move.lang.MoveFileType
import org.move.lang.core.psi.MvModule
import org.move.lang.core.types.*
import org.move.lang.core.types.Address.Named
import org.move.lang.toMoveFile

class MvModuleFileIndex: ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> = INDEX_ID
    override fun getVersion(): Int = 1
    override fun dependsOnFileContent(): Boolean = true

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(MoveFileType)

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return object: DataIndexer<String, Void?, FileContent> {
            override fun map(inputData: FileContent): Map<String, Void?> {
                val file = inputData.psiFile as MoveFile
                // build a list of all modules for which there is module specs
                val pathModuleIds = file.modules().flatMap { it.indexIds() }
                return pathModuleIds.associate { it to null }
            }
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String?> = EnumeratorStringDescriptor.INSTANCE

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

            val prefixes = addressIndexIds(address).map { "$it::" }
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
            val indexIds = moduleIndexIds(address, moduleName)
            return buildList {
                val vFiles =
                    filesIndex.getContainingFilesForAnyKey(INDEX_ID, indexIds, searchScope)
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
    val moduleAddress = this.address(moveProject)
    val sameValues = Address.equals(moduleAddress, address)

    if (sameValues && isCompletion) {
        // compare named addresses by name in case of the same values for the completion
        if (address is Named && moduleAddress is Named) {
            return address.name == moduleAddress.name
        }
    }

    return sameValues
}
