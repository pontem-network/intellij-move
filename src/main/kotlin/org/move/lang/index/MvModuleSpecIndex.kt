package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingMoveFile
import org.move.lang.isTempFile
import org.move.lang.moduleSpecs
import org.move.lang.toMoveFile
import org.move.openapiext.common.isUnitTestMode

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
            module: MvModule,
            searchScope: GlobalSearchScope
        ): List<MoveFile> {
            if (isUnitTestMode) {
                val moduleFile = module.containingMoveFile?.takeIf { it.isTempFile() }
                if (moduleFile != null) {
                    return listOf(moduleFile)
                }
            }

            val moduleName = module.name ?: return emptyList()
            val fileIndex = FileBasedIndex.getInstance()
            return fileIndex
                .getContainingFiles(KEY, moduleName, searchScope)
                .mapNotNull { it.toMoveFile(project) }
                .toList()
        }
    }
}
