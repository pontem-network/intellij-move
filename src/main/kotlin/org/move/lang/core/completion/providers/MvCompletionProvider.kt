package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.utils.imports.ImportCandidate
import org.move.ide.utils.imports.ImportCandidateCollector
import org.move.ide.utils.imports.import
import org.move.lang.core.completion.DefaultInsertHandler
import org.move.lang.core.completion.getElementOfType
import org.move.lang.core.psi.MvElement
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
            val names = MvNamedElementIndex.getAllKeys(project)
            addAll(names)
            removeAll(processedPathNames)
        }

        return result.prefixMatcher.sortMatching(keys)
            .flatMap {
                ImportCandidateCollector
                    .getImportCandidates(importContext, it)
                    .distinctBy { it.element }
                    .filter { itemFilter(it.element) }
            }
    }
}

class ImportInsertHandler(
    val parameters: CompletionParameters,
    private val candidate: ImportCandidate
) : DefaultInsertHandler() {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)
//        context.commitDocument()
//        val path = parameters.originalPosition?.parent as? MvPath ?: return
        context.import(candidate)
//        candidate.import(path)
    }
}

fun InsertionContext.import(candidate: ImportCandidate) {
//    if (RsCodeInsightSettings.getInstance().importOutOfScopeItems) {
    commitDocument()
    getElementOfType<MvElement>()?.let { candidate.import(it) }
//    }
}
