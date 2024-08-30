package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.safeGetOriginalOrSelf
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve2.ref.ResolutionContext

object CommonCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() = PlatformPatterns.psiElement().withParent(psiElement<MvReferenceElement>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // Use original position if possible to re-use caches of the real file
        val position = parameters.position.safeGetOriginalOrSelf()
        val element = position.parent as MvReferenceElement
        if (position !== element.referenceNameElement) return

        val msl = element.isMsl()
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(element, msl)
        val resolutionCtx = ResolutionContext(element, true)

//        val completionContext = CompletionContext(
//            element,
//            msl,
//            expectedTy,
//            resolutionCtx = resolutionCtx,
//            structAsType,
//        )

    }

//    @VisibleForTesting
//    fun addCompletionVariants(
//        element: MvReferenceElement,
//        result: CompletionResultSet,
//        context: CompletionContext,
//        processedPathNames: MutableSet<String>
//    ) {
//        collectCompletionVariants(result, context) {
//
//        }
//    }
}