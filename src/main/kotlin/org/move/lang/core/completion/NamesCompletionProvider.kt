package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.psi.MoveSchemaReferenceElement
import org.move.lang.core.psi.MoveTypeReferenceElement
import org.move.lang.core.psi.ext.isSpecElement
import org.move.lang.core.resolve.processNestedScopesUpwards
import org.move.lang.core.resolve.ref.Namespace

object NamesCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            PlatformPatterns.or(
                MovePsiPatterns.qualifiedPathIdentifier(),
                MovePsiPatterns.specIdentifier()
            )
//    override val elementPattern: ElementPattern<PsiElement>
//        get() =
//            PlatformPatterns.psiElement().withSuperParent()
//                .andNot(PlatformPatterns.psiElement()
//                    .withSuperParent<MoveQualifiedPathType>(2))

//    override val elementPattern: ElementPattern<PsiElement>
//        get() {
//            val directRefIdentifier =
//                PlatformPatterns.psiElement()
//                    .withElementType(MoveElementTypes.IDENTIFIER)
//                    .withParent(psiElement<MoveReferenceElement>())
//            val qualifiedPathIdentifier =
//                PlatformPatterns.psiElement()
//                    .withElementType(MoveElementTypes.IDENTIFIER)
//                    .withParent(psiElement<MoveQualifiedPath>())
//                    .withSuperParent<MoveReferenceElement>(2)
//            return PlatformPatterns.or(directRefIdentifier, qualifiedPathIdentifier)
//        }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val directParent = parameters.position.parent
        val refElement = directParent as? MoveReferenceElement ?: directParent.parent as MoveReferenceElement

        if (parameters.position !== refElement.referenceNameElement) return

        val namespace = when (refElement) {
            is MoveTypeReferenceElement -> Namespace.TYPE
            is MoveSchemaReferenceElement -> Namespace.SCHEMA
            else -> Namespace.NAME
        }
        processNestedScopesUpwards(refElement, namespace) {
            if (it.element != null) {
                val lookup = it.element.createLookupElement(refElement.isSpecElement())
                result.addElement(lookup)
            }
            false
        }
    }
}