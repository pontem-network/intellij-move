package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvPatField
import org.move.lang.core.psi.ext.fieldNames
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.parentPatStruct
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.collectCompletionVariants
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.wrapWithFilter
import org.move.lang.core.resolve2.processPatBindingResolveVariants

object ReferenceCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns.psiElement().withParent(psiElement<MvReferenceElement>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // Use original position if possible to re-use caches of the real file
        val position = parameters.position
        val element = position.parent as MvReferenceElement
        if (position !== element.referenceNameElement) return

        val msl = element.isMsl()
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(element, msl)

        val completionCtx = MvCompletionContext(element, msl, expectedTy)

        addCompletionVariants(element, result, completionCtx)
    }

    fun addCompletionVariants(
        element: MvReferenceElement,
        result: CompletionResultSet,
        context: MvCompletionContext,
    ) {
        collectCompletionVariants(result, context) {
            val processor0 = filterCompletionVariantsByVisibility(element, it)
            // todo: filter test functions
            when (element) {
                // `let Res/*caret*/ =`
                // catches all modules, enums, enum variants and struct patterns
                is MvPatBinding -> {
                    // for struct pat / lit, it filters out all the fields already existing in the body
                    val processor = skipAlreadyProvidedFields(element, processor0)
                    processPatBindingResolveVariants(element, true, processor)
                }
            }
        }
    }
}

private fun skipAlreadyProvidedFields(
    refElement: MvReferenceElement,
    processor0: RsResolveProcessor
): RsResolveProcessor {
    val parent = refElement.parent
    val providedFieldNames = when (parent) {
        // shorthand, skip all provided fields
        is MvPatField -> parent.parentPatStruct.fieldNames
//                    is MvStructLitField -> parent.parentStructLitExpr.providedFieldNames
        else -> emptySet()
    }
    return processor0.wrapWithFilter { e -> e.name !in providedFieldNames }
}