package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.processNestedScopesUpwards
import org.move.lang.core.resolve.ref.Namespace

object CommonCompletionProvider : CompletionProvider<CompletionParameters>() {
    val elementPattern: ElementPattern<PsiElement>
        get() {
            val directRefIdentifier =
                PlatformPatterns.psiElement()
                    .withElementType(MoveElementTypes.IDENTIFIER)
                    .withParent(psiElement<MoveReferenceElement>())
            val qualifiedPathIdentifier =
                PlatformPatterns.psiElement()
                    .withElementType(MoveElementTypes.IDENTIFIER)
                    .withParent(psiElement<MoveQualifiedPath>())
                    .withSuperParent(2, psiElement<MoveReferenceElement>())
            return PlatformPatterns.or(directRefIdentifier, qualifiedPathIdentifier)
        }

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
                val isSpec = refElement is MoveFunctionSpec || refElement is MoveStructSpec
                result.addElement(it.element.createLookupElement(isSpec))
            }
            false
        }
    }
}