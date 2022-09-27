package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvQualifiedNamedElement
import org.move.lang.toMoveFile

class MvNamedElementIndex : BaseMoveFileIndex() {
    override fun getName() = KEY
    override fun getVersion() = INDEX_VERSION
    override fun getIndexer() =
        DataIndexer<String, Void, FileContent> { fileContent ->
            val file = fileContent.psiFile as? MoveFile ?: return@DataIndexer emptyMap()
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

        fun namedElementFiles(
            project: Project,
            targetName: String,
            searchScope: GlobalSearchScope
        ): Collection<MoveFile> {
            val fileIndex = FileBasedIndex.getInstance()
            val files = fileIndex.getContainingFiles(KEY, targetName, searchScope)
            return files.mapNotNull { it.toMoveFile(project) }
        }
    }
}
