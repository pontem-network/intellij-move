package org.move.lang.core.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.move.ide.inspections.imports.AutoImportFix
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.inspections.imports.MvNamedElementIndex
import org.move.ide.inspections.imports.import
import org.move.lang.core.psi.MvPath

abstract class MvCompletionProvider : CompletionProvider<CompletionParameters>() {
    abstract val elementPattern: ElementPattern<out PsiElement>

    protected fun addCompletionsFromIndex(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        processedPathNames: Set<String>,
        importContext: ImportContext,
    ) {
        val project = parameters.position.project
        val keys = hashSetOf<String>().apply {
            val names = MvNamedElementIndex.getAllKeys(project)
            addAll(names)
            removeAll(processedPathNames)
        }
        for (elementName in result.prefixMatcher.sortMatching(keys)) {
            val candidates = AutoImportFix.getImportCandidates(importContext, elementName)
            candidates
                .distinctBy { it.element }
                .map { candidate ->
                    val element = candidate.element
                    val insertHandler = object : MvInsertHandler() {
                        override fun handleInsert(context: InsertionContext, item: LookupElement) {
                            super.handleInsert(context, item)
                            context.commitDocument()
                            val path = parameters.originalPosition?.parent as? MvPath ?: return
                            candidate.import(path)
                        }
                    }
                    element.createCompletionLookupElement(
                        insertHandler,
                        importContext.itemVis.namespaces
                    )
                }
                .forEach(result::addElement)
        }
    }
}
