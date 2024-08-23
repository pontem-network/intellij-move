package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPattern

object BoolsCompletionProvider : MvCompletionProvider() {

    override val elementPattern: ElementPattern<out PsiElement> get() = MvPsiPattern.simplePathPattern

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val lookups =
            listOf("true", "false").map { LookupElementBuilder.create(it).bold() }
        result.addAllElements(lookups)
    }
}
