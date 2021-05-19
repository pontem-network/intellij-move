package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.MoveFullyQualifiedModuleRef
import org.move.lang.core.resolve.processQualModuleRef
import org.move.lang.core.withParent

object QualModulesCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns.psiElement()
                .withParent<MoveFullyQualifiedModuleRef>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val directParent = parameters.position.parent
        val fullyQualifiedModuleRef =
            directParent as? MoveFullyQualifiedModuleRef
                ?: directParent.parent as MoveFullyQualifiedModuleRef
        if (parameters.position !== fullyQualifiedModuleRef.referenceNameElement) return

        processQualModuleRef(fullyQualifiedModuleRef) {
            if (it.element != null) {
                val lookup = it.element.createLookupElement(false)
                result.addElement(lookup)
            }
            false
        }
    }
}
