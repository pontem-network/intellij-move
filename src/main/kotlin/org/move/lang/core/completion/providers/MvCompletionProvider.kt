package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.move.ide.inspections.imports.AutoImportFix
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.inspections.imports.MoveElementsIndex
import org.move.ide.inspections.imports.import
import org.move.lang.core.completion.MvDefaultInsertHandler
import org.move.lang.core.completion.UNIMPORTED_ITEM_PRIORITY
import org.move.lang.core.completion.createCompletionLookupElement
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvPath
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

abstract class MvCompletionProvider : CompletionProvider<CompletionParameters>() {
    abstract val elementPattern: ElementPattern<out PsiElement>

    protected fun addCompletionsFromIndex(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        processedPathNames: Set<String>,
        importContext: ImportContext,
        itemFilter: (PsiElement) -> Boolean = { true }
    ) {
        val project = parameters.position.project
        val keys = hashSetOf<String>().apply {
            val names = MoveElementsIndex.getAllKeys(project)
            addAll(names)
            removeAll(processedPathNames)
        }
        for (elementName in result.prefixMatcher.sortMatching(keys)) {
            val candidates = AutoImportFix.getImportCandidates(importContext, elementName)
            candidates
                .distinctBy { it.element }
                .filter { itemFilter(it.element) }
                .map { candidate ->
                    val element = candidate.element
                    val insertHandler = object : MvDefaultInsertHandler() {
                        override fun handleInsert(context: InsertionContext, item: LookupElement) {
                            super.handleInsert(context, item)
                            context.commitDocument()
                            val path = parameters.originalPosition?.parent as? MvPath ?: return
                            candidate.import(path)
                        }
                    }
                    element.createCompletionLookupElement(
                        insertHandler,
                        importContext.itemVis.namespaces,
                        priority = UNIMPORTED_ITEM_PRIORITY,
                    )
                }
                .forEach(result::addElement)
        }
    }
}

data class MvCompletionContext(
    val context: MvElement? = null,
    val expectedTy: Ty = TyUnknown,
)
