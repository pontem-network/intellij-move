package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPattern
import org.move.lang.core.completion.KEYWORD_PRIORITY

object AbilitiesCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> get() = MvPsiPattern.ability()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val abilities = arrayOf("copy", "store", "drop", "key")
        for (ability in abilities) {
            val element = LookupElementBuilder.create(ability)
            result.addElement(PrioritizedLookupElement.withPriority(element, KEYWORD_PRIORITY))
        }
    }
}
