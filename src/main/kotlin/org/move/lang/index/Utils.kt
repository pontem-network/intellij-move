package org.move.lang.index

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.CommonProcessors
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID

fun <K, V> FileBasedIndex.getContainingFilesForAnyKey(
    indexId: ID<K, V>,
    dataKeys: HashSet<K>,
    searchScope: GlobalSearchScope
): List<VirtualFile> {
    val collectProcessor = CommonProcessors.CollectProcessor(mutableListOf<VirtualFile>())
    this.processFilesContainingAnyKey(
        indexId,
        dataKeys,
        searchScope,
        null,
        null,
        collectProcessor
    )
    return collectProcessor.results.distinct().toList()
}