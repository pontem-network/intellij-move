package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.move.ide.utils.imports.ImportCandidate
import org.move.ide.utils.imports.import
import org.move.lang.core.completion.DefaultInsertHandler
import org.move.lang.core.completion.getElementOfType
import org.move.lang.core.psi.MvElement

abstract class MvCompletionProvider: CompletionProvider<CompletionParameters>() {
    abstract val elementPattern: ElementPattern<out PsiElement>
}

class ImportInsertHandler(
    val parameters: CompletionParameters,
    private val candidate: ImportCandidate
): DefaultInsertHandler() {

    override fun handleInsert(element: MvElement, context: InsertionContext, item: LookupElement) {
        super.handleInsert(element, context, item)
        context.import(candidate)
    }
}

fun InsertionContext.import(candidate: ImportCandidate) {
//    if (RsCodeInsightSettings.getInstance().importOutOfScopeItems) {
    commitDocument()
    getElementOfType<MvElement>()?.let { candidate.import(it) }
//    }
}
