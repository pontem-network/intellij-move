package org.move.lang.index

import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import org.move.cli.MoveProject
import org.move.lang.MoveFile
import org.move.lang.core.resolve.ref.Ns
import org.move.lang.core.resolve.ref.NsSet
import org.move.lang.core.types.ItemFQName
import org.move.lang.core.types.fqName

private fun nsToString(ns: NsSet): String {
    return ns.joinToString("#", transform = { it.name })
}

private fun nsFromString(s: String): NsSet {
    val nss = s.split("#").map { Ns.valueOf(it) }
    return NsSet.copyOf(nss)
}

class MvItemNamespaceIndex: MvFileIndexExtension<String>() {
    override fun getName(): ID<String, String> = INDEX_ID
    override fun getVersion(): Int = 4

    override fun getValueExternalizer(): DataExternalizer<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getIndexer(): DataIndexer<String, String, FileContent> {
        return object: DataIndexer<String, String, FileContent> {
            override fun map(inputData: FileContent): Map<String, String> {
                val file = inputData.psiFile as MoveFile
                // build a list of all modules for which there is module specs
                val entries = file.importableEntries()
                return buildMap {
                    for (entry in entries) {
                        val fqName = entry.element()?.fqName() ?: continue
                        val indexId = fqName.indexId()
                        set(indexId, entry.ns.name)
                    }
                }
            }
        }
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        val INDEX_ID: ID<String, String> = ID.create("org.move.index.MvItemNamespaceIndex")

        fun getItemNs(moveProject: MoveProject, fqName: ItemFQName): Ns? {
            val filesIndex = FileBasedIndex.getInstance()
            val searchScope = moveProject.searchScope()
            val indexIds = fqName.searchIndexIds(moveProject)
            val nsName =
                filesIndex.getValuesForAnyKey(INDEX_ID, indexIds, searchScope)
                    // should be always a single value
                    .firstOrNull()
            return nsName?.let { Ns.valueOf(it) }
//            return if (nsName == null) NONE else Ns.valueOf(nsName)
        }
    }
}