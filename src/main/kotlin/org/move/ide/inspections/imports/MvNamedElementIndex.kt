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
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.processModuleItems
import org.move.lang.modules
import org.move.lang.toMvFile

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
    }
}

fun MvFile.qualifiedItems(target: String, itemVis: ItemVis): List<MvQualifiedNamedElement> {
    val elements = mutableListOf<MvQualifiedNamedElement>()
    val modules = this.modules()
    for (module in modules) {
        if (Namespace.MODULE in itemVis.namespaces) {
            if (module.name == target) {
                elements.add(module)
            }
        }
        processModuleItems(module, itemVis) {
            val element = it.element
            if (element is MvQualifiedNamedElement && element.name == target) {
                elements.add(element)
            }
            false
        }
    }
    return elements
}
