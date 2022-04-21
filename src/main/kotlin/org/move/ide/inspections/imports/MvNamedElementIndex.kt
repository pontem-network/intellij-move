package org.move.ide.inspections.imports

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.move.lang.MoveFileType
import org.move.lang.MvFile
import org.move.lang.core.psi.MvQualifiedNamedElement
import org.move.lang.core.psi.ext.functions
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.modules
import org.move.lang.toMvFile
import org.toml.lang.psi.TomlFileType

class MvNamedElementIndex : ScalarIndexExtension<String>() {
    override fun getName() = KEY
    override fun getVersion() = INDEX_VERSION
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    override fun dependsOnFileContent() = true

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(MoveFileType)

    override fun getIndexer() =
        DataIndexer<String, Void, FileContent> { data ->
            val file = data.psiFile as? MvFile ?: return@DataIndexer emptyMap()
            val map = file
                .descendantsOfType<MvQualifiedNamedElement>()
                .mapNotNull { it.name }
                .associateWith { null }
            map
        }

    companion object {
        const val INDEX_VERSION = 1

        val KEY = ID.create<String, Void>("ModulesIndex")

        fun findFilesByElementName(
            project: Project,
            target: String,
            searchScope: GlobalSearchScope
        ): Collection<MvFile> {
            val fileIndex = FileBasedIndex.getInstance()
            val files = fileIndex.getContainingFiles(KEY, target, searchScope)
            return files.mapNotNull { it.toMvFile(project) }
        }

//        fun findElementsByName(
//            project: Project,
//            target: String,
//            searchScope: GlobalSearchScope
//        ): List<MvQualifiedNamedElement> {
//            val files = findFilesByElementName(project, target, searchScope)
//            return files.flatMap { it.qualifiedItems() }
//        }
    }
}

fun MvFile.qualifiedItems(): Sequence<MvQualifiedNamedElement> {
    return sequenceOf(
        this.modules(),
        this.modules().flatMap { it.functions(Visibility.Public) }
    ).flatten()
}
