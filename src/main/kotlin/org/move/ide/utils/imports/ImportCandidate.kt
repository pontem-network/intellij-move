package org.move.ide.utils.imports

import org.move.ide.inspections.imports.ImportContext
import org.move.ide.inspections.imports.qualifiedItems
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvQualNamedElement
import org.move.lang.core.resolve.processQualItem
import org.move.lang.core.types.ItemQualName
import org.move.lang.index.MvNamedElementIndex
import org.move.lang.moveProject
import org.move.openapiext.common.isUnitTestMode

data class ImportCandidate(val element: MvQualNamedElement, val qualName: ItemQualName)

object ImportCandidateCollector {
    fun getImportCandidates(
        context: ImportContext,
        targetName: String,
        itemFilter: (MvQualNamedElement) -> Boolean = { true }
    ): List<ImportCandidate> {
        val (contextElement, namespaces, itemVis) = context

        val project = contextElement.project
        val moveProject = contextElement.moveProject ?: return emptyList()
        val searchScope = moveProject.searchScope()

        val allItems = mutableListOf<MvQualNamedElement>()
        if (isUnitTestMode) {
            // always add current file in tests
            val currentFile = contextElement.containingFile as? MoveFile ?: return emptyList()
            val items = currentFile.qualifiedItems(targetName, namespaces, itemVis)
            allItems.addAll(items)
        }

        MvNamedElementIndex
            .processElementsByName(project, targetName, searchScope) { element ->
//                val namespaces = itemVis.namespaces
                processQualItem(element, namespaces, itemVis) {
                    val entryElement = it.element
                    if (entryElement !is MvQualNamedElement) return@processQualItem false
                    if (it.name == targetName) {
                        allItems.add(entryElement)
                    }
                    false
                }
                true
            }

        return allItems
            .filter(itemFilter)
            .mapNotNull { item -> item.qualName?.let { ImportCandidate(item, it) } }
    }
}
