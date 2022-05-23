package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.move.ide.inspections.imports.AutoImportFix
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.inspections.imports.MvNamedElementIndex
import org.move.ide.inspections.imports.import

abstract class MvCompletionProvider : CompletionProvider<CompletionParameters>() {
    abstract val elementPattern: ElementPattern<out PsiElement>

    protected fun addCompletionsFromIndex(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        processedPathNames: Set<String>,
        importContext: ImportContext
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
                    element.createLookupElement().withInsertHandler { _, _ ->
                        candidate.import(importContext.pathElement)
                    }
                }
                .forEach(result::addElement)
        }
    }
}
