package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.VisibleForTesting
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.tyInfers
import org.move.lang.core.resolve.collectCompletionVariants
import org.move.lang.core.resolve.createProcessor
import org.move.lang.core.resolve2.processMethodResolveVariants
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.substitute
import org.move.lang.core.types.ty.*
import org.move.lang.core.withParent

object MethodOrFieldCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns
                .psiElement()
                .withParent<MvMethodOrField>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val pos = parameters.position
        val element = pos.parent as MvMethodOrField

        addMethodOrFieldVariants(element, result)
    }

    @VisibleForTesting
    fun addMethodOrFieldVariants(element: MvMethodOrField, result: CompletionResultSet) {
        val msl = element.isMsl()
        val receiverTy = element.inferReceiverTy(msl).knownOrNull() ?: return
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(element, msl)

        val ctx = CompletionContext(element, msl, expectedTy)

        val tyAdt = receiverTy.derefIfNeeded() as? TyAdt
        if (tyAdt != null) {
            collectCompletionVariants(result, ctx, subst = tyAdt.substitution) {
                processNamedFieldVariants(element, tyAdt, msl, it)
            }
        }

        processMethodResolveVariants(element, receiverTy, ctx.msl, createProcessor { e ->
            val function = e.element as? MvFunction ?: return@createProcessor
            val subst = function.tyInfers
            val declaredFuncTy = function.declaredType(msl).substitute(subst) as TyFunction
            val declaredSelfTy = declaredFuncTy.paramTypes.first()
            val autoborrowedReceiverTy =
                TyReference.autoborrow(receiverTy, declaredSelfTy)
                    ?: error("unreachable, references always compatible")

            val inferenceCtx = InferenceContext(msl)
            inferenceCtx.combineTypes(declaredSelfTy, autoborrowedReceiverTy)

            result.addElement(
                createLookupElement(
                    e,
                    ctx,
                    subst = inferenceCtx.resolveTypeVarsIfPossible(subst)
                )
            )
        })
    }
}