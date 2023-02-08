package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.move.ide.inspections.imports.AutoImportFix
import org.move.ide.inspections.imports.ImportCandidate
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.index.MvNamedElementIndex

abstract class MvCompletionProvider : CompletionProvider<CompletionParameters>() {
    abstract val elementPattern: ElementPattern<out PsiElement>

    protected fun getImportCandidates(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        processedPathNames: Set<String>,
        importContext: ImportContext,
        itemFilter: (PsiElement) -> Boolean = { true }
    ): List<ImportCandidate> {
        val project = parameters.position.project
        val keys = hashSetOf<String>().apply {
            val names = MvNamedElementIndex.getAllNames(project)
            addAll(names)
            removeAll(processedPathNames)
        }
        return result.prefixMatcher.sortMatching(keys)
            .flatMap {
                AutoImportFix.getImportCandidates(importContext, it)
                    .distinctBy { it.element }
                    .filter { itemFilter(it.element) }
            }
    }
}
