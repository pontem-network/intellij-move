package org.move.ide.inspections.imports

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.move.lang.MoveFile
import org.move.lang.MoveFileType
import org.move.lang.core.psi.MvQualifiedNamedElement
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.processFileItems
import org.move.lang.toMoveFile

class MoveElementsIndex : ScalarIndexExtension<String>() {
    override fun getName() = KEY
    override fun getVersion() = INDEX_VERSION
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    override fun dependsOnFileContent() = true
    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(MoveFileType)

    override fun getIndexer() =
        DataIndexer<String, Void, FileContent> { data ->
            val file = data.psiFile as? MoveFile ?: return@DataIndexer emptyMap()
            val map = file
                .descendantsOfType<MvQualifiedNamedElement>()
                .mapNotNull { it.name }
                .associateWith { null }
            map
        }

    companion object {
        const val INDEX_VERSION = 1

        val KEY = ID.create<String, Void>("MvNamedElementIndex")

        fun requestRebuild() {
            FileBasedIndex.getInstance().requestRebuild(KEY)
        }

        fun getAllKeys(project: Project): Collection<String> {
            return FileBasedIndex.getInstance().getAllKeys(KEY, project)
        }

        fun findFilesByElementName(
            project: Project,
            target: String,
            searchScope: GlobalSearchScope
        ): Collection<MoveFile> {
            val fileIndex = FileBasedIndex.getInstance()
            val files = fileIndex.getContainingFiles(KEY, target, searchScope)
            return files.mapNotNull { it.toMoveFile(project) }
        }
    }
}

fun MoveFile.qualifiedItems(targetName: String, itemVis: ItemVis): List<MvQualifiedNamedElement> {
    val elements = mutableListOf<MvQualifiedNamedElement>()
    processFileItems(this, itemVis) {
        if (it.element is MvQualifiedNamedElement && it.name == targetName) {
            elements.add(it.element)
        }
        false
    }
    return elements
}
