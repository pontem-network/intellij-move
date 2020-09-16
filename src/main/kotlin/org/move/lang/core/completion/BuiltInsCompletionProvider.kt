package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.core.MovePsiPatterns

object BuiltInsCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MovePsiPatterns.qualifiedPathIdentifier()
            .andNot(MovePsiPatterns.qualifiedPathTypeIdentifier())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val functions = BUILTIN_FUNCTIONS
        functions.forEach {
            val lookup = LookupElementBuilder.create(it)
            result.addElement(lookup.withPriority(BUILTIN_FUNCTION_PRIORITY))
        }
    }
}