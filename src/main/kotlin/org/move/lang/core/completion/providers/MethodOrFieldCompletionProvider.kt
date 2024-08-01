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
import org.move.lang.core.psi.MvMethodCall
import org.move.lang.core.psi.ext.MvMethodOrField
import org.move.lang.core.psi.ext.getFieldVariants
import org.move.lang.core.psi.ext.inferReceiverTy
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.tyInfers
import org.move.lang.core.resolve.createProcessor
import org.move.lang.core.resolve2.processMethodResolveVariants
import org.move.lang.core.resolve2.ref.ResolutionContext
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.substitute
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.knownOrNull
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
//        val scopeInfo = ContextScopeInfo(
//            letStmtScope = element.letStmtScope,
//            refItemScopes = element.refItemScopes,
//        )
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(element, msl)

        val ctx = CompletionContext(element, msl, expectedTy)
//        val ctx = CompletionContext(element, scopeInfo, msl, expectedTy)

        val structTy = receiverTy.derefIfNeeded() as? TyStruct
        if (structTy != null) {
            getFieldVariants(element, structTy, msl)
                .forEach { (_, field) ->
                    val lookupElement = field.createLookupElement(
                        ctx,
                        subst = structTy.substitution
                    )
                    result.addElement(lookupElement)
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