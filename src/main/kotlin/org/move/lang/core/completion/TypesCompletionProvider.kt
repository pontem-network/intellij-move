package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.psi.MoveFunctionSpec
import org.move.lang.core.psi.MoveStructSpec
import org.move.lang.core.psi.MoveTypeReferenceElement
import org.move.lang.core.resolve.processNestedScopesUpwards
import org.move.lang.core.resolve.ref.Namespace

object TypesCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MovePsiPatterns.qualifiedPathTypeIdentifier()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val directParent = parameters.position.parent
        val refElement =
            directParent as? MoveTypeReferenceElement ?: directParent.parent as MoveTypeReferenceElement

        if (parameters.position !== refElement.referenceNameElement) return

        processNestedScopesUpwards(refElement, Namespace.TYPE) {
            if (it.element != null) {
                val isSpec = refElement is MoveFunctionSpec || refElement is MoveStructSpec
                result.addElement(it.element.createLookupElement(isSpec))
            }
            false
        }

    }

}