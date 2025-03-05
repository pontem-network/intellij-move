package org.move.lang.index

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.CommonProcessors
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.VoidDataExternalizer
import org.jetbrains.annotations.NotNull
import org.move.lang.MoveFileType

abstract class MvFileIndexExtension<V>: FileBasedIndexExtension<String, V>() {
    // override in subclass to change
    override fun getVersion(): Int = 1
    override fun dependsOnFileContent(): Boolean = true
    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(MoveFileType)
    override fun getKeyDescriptor(): KeyDescriptor<String?> = EnumeratorStringDescriptor.INSTANCE
}

abstract class MvScalarFileIndexExtension: MvFileIndexExtension<Void>() {
    override fun getValueExternalizer(): DataExternalizer<Void> = VoidDataExternalizer.INSTANCE
}

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

fun <V> FileBasedIndex.getValuesForAnyKey(
    indexId: ID<String, V>,
    dataKeys: Collection<String>,
    searchScope: GlobalSearchScope
): List<V> {
    val fileIndex = this
    val values = buildList {
        for (dataKey in dataKeys) {
            val values = fileIndex.getValues(indexId, dataKey, searchScope)
            addAll(values)
        }
    }
    return values.distinct()
}