package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.psi.MoveQualPathReferenceElement
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace

object ModulesCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MovePsiPatterns.qualPathIdentifier()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val directParent = parameters.position.parent
        val refElement =
            directParent as? MoveQualPathReferenceElement
                ?: directParent.parent as MoveQualPathReferenceElement

        if (parameters.position !== refElement.referenceNameElement) return
        if (refElement.qualPath.moduleRef != null) return

        processItems(refElement, Namespace.MODULE) {
            if (it.element != null) {
                val lookup = it.element.createLookupElement(false)
                result.addElement(lookup)
            }
            false
        }
    }
}
