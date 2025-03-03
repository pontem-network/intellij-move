package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import org.move.lang.MoveFile
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntry
import org.move.lang.core.resolve.scopeEntry.filterByName
import org.move.lang.core.resolve.ref.Ns
import org.move.lang.core.resolve.ref.NsSet
import org.move.lang.core.resolve.ref.filterByNs
import org.move.lang.core.resolve.scopeEntry.itemEntries
import org.move.lang.toMoveFile

class MvNamedItemFilesIndex: MvScalarFileIndexExtension() {
    override fun getName(): ID<String, Void> = INDEX_ID

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return object: DataIndexer<String, Void?, FileContent> {
            override fun map(inputData: FileContent): Map<String, Void?> {
                val file = inputData.psiFile as MoveFile
                // build a list of all modules for which there is module specs
                val itemIndexIds = file.importableEntries()
                    .flatMap { namedIndexIds(it.name, it.namespaces) }
                return itemIndexIds.associate { it to null }
            }
        }
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        val INDEX_ID: ID<String, Void> = ID.create("org.move.index.MvNamedElementFileIndex")

        fun getAllItemNames(project: Project, ns: NsSet): Set<String> {
            val filesIndex = FileBasedIndex.getInstance()
            val itemIds = filesIndex.getAllKeys(INDEX_ID, project)
            val matchedItemIds = itemIds
                .filter { name ->
                    ns.any { name.endsWith("::$it") }
                }
            return matchedItemIds.map { it.split("::")[0] }.toSet()
        }

        fun getEntriesFor(
            project: Project,
            searchScope: GlobalSearchScope,
            targetNames: List<String>,
            ns: NsSet
        ): List<ScopeEntry> {
            val indexIds = targetNames.flatMap { namedIndexIds(it, ns) }.toHashSet()

            val filesIndex = FileBasedIndex.getInstance()
            return buildList {
                val vFiles =
                    filesIndex.getContainingFilesForAnyKey(INDEX_ID, indexIds, searchScope)
                val files = vFiles.mapNotNull { it.toMoveFile(project) }
                for (file in files) {
                    val entries = file.importableEntries().filterByNs(ns)
                    for (targetName in targetNames) {
                        addAll(entries.filterByName(targetName))
                    }
                }
            }
        }

        private fun namedIndexIds(name: String, ns: NsSet): HashSet<String> {
            return ns.map { "$name::${it.name}" }.toHashSet()
        }
    }
}

private fun MoveFile.importableEntries(): List<ScopeEntry> {
    val file = this
    return buildList {
        for (module in file.modules()) {
            val moduleEntry = module.asEntry() ?: continue
            add(moduleEntry)
            addAll(module.itemEntries)
        }
    }
}