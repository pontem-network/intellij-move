package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import org.move.lang.MoveFile
import org.move.lang.moduleSpecs
import org.move.lang.toMoveFile

class MvModuleSpecIndex: BaseMoveFileIndex() {
    override fun getName() = KEY
    override fun getVersion() = INDEX_VERSION
    override fun getIndexer() =
        DataIndexer<String, Void, FileContent> { data ->
            val file = data.psiFile as? MoveFile ?: return@DataIndexer emptyMap()
            // create (moduleName -> null) map for every file
            file.moduleSpecs()
                .mapNotNull { it.fqModuleRef?.referenceName }
                .associateWith { null }
        }

    companion object {
        const val INDEX_VERSION = 1

        val KEY = ID.create<String, Void>("MvModuleSpecIndex")

        fun requestRebuild() {
            FileBasedIndex.getInstance().requestRebuild(KEY)
        }

        fun getAllKeys(project: Project): Collection<String> {
            return FileBasedIndex.getInstance().getAllKeys(MvNamedElementIndex.KEY, project)
        }

        fun moduleSpecFiles(
            project: Project,
            moduleName: String,
            searchScope: GlobalSearchScope
        ): Collection<MoveFile> {
            val fileIndex = FileBasedIndex.getInstance()
            val files = fileIndex.getContainingFiles(KEY, moduleName, searchScope)
            return files.mapNotNull { it.toMoveFile(project) }
        }
    }
}
