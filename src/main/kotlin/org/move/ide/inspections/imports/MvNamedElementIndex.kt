package org.move.ide.inspections.imports

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
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

class MvNamedElementIndex : ScalarIndexExtension<String>() {
    override fun getName() = INDEX_ID
    override fun getVersion() = INDEX_VERSION
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    override fun dependsOnFileContent() = true
    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(MoveFileType)

    override fun getIndexer() =
        DataIndexer<String, Void, FileContent> { data ->
            val map = mutableMapOf<String, Void?>()
            val file = data.psiFile as MvFile
            file.processChildren { child ->
                PsiTreeUtil.processElements(child, MvQualifiedNamedElement::class.java) {
                    val name = it.name ?: return@processElements true
                    map[name] = null
                    true
                }
                true
            }
            map
        }

    companion object {
        const val INDEX_VERSION = 1

        val INDEX_ID = ID.create<String, Void>("ModulesIndex")

        fun findFilesByElementName(
            project: Project,
            target: String,
            searchScope: GlobalSearchScope
        ): Collection<MvFile> {
            val files = FileBasedIndex.getInstance().getContainingFiles(INDEX_ID, target, searchScope)
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
