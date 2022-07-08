package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.move.ide.inspections.imports.*
import org.move.lang.core.psi.ext.ancestors
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.resolve.MvReferenceElement
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

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
            val names = MoveElementsIndex.getAllKeys(project)
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
