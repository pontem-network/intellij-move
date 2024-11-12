package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.VisibleForTesting
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.collectCompletionVariants
import org.move.lang.core.resolve.createProcessor
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.processItemSpecRefResolveVariants
import org.move.lang.core.resolve.wrapWithFilter
import org.move.lang.core.resolve2.processFieldLookupResolveVariants
import org.move.lang.core.resolve2.processLabelResolveVariants
import org.move.lang.core.resolve2.processMethodResolveVariants
import org.move.lang.core.resolve2.processPatBindingResolveVariants
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.substitute
import org.move.lang.core.types.ty.*

object CommonCompletionProvider: MvCompletionProvider() {
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

        // handles dot expr
        if (element is MvMethodOrField) {
            addMethodOrFieldVariants(element, result)
        }

        addCompletionVariants(element, result, completionCtx)
    }

    fun addCompletionVariants(
        element: MvReferenceElement,
        result: CompletionResultSet,
        completionCtx: MvCompletionContext,
    ) {
        collectCompletionVariants(result, completionCtx) {
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
                // loop labels
                is MvLabel -> processLabelResolveVariants(element, it)
                // `spec ITEM {}` module items, where ITEM is a reference to the function/struct/enum
                is MvItemSpecRef -> processItemSpecRefResolveVariants(element, it)
            }
        }
    }

    @VisibleForTesting
    fun addMethodOrFieldVariants(element: MvMethodOrField, result: CompletionResultSet) {
        val msl = element.isMsl()
        val receiverTy = element.inferReceiverTy(msl).knownOrNull() ?: return
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(element, msl)

        val ctx = MvCompletionContext(element, msl, expectedTy)

        val tyAdt = receiverTy.derefIfNeeded() as? TyAdt
        if (tyAdt != null) {
            collectCompletionVariants(result, ctx, subst = tyAdt.substitution) {
                processFieldLookupResolveVariants(element, tyAdt, msl, it)
            }
        }

        processMethodResolveVariants(element, receiverTy, ctx.msl, createProcessor { e ->
            val function = e.element as? MvFunction ?: return@createProcessor
            val subst = function.tyVarsSubst
            val declaredFuncTy = function.functionTy(msl).substitute(subst) as TyFunction
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

private fun skipAlreadyProvidedFields(
    refElement: MvReferenceElement,
    processor0: RsResolveProcessor
): RsResolveProcessor {
    val parent = refElement.parent
    val providedFieldNames = when (parent) {
        // shorthand, skip all provided fields
        is MvPatField -> parent.patStruct.fieldNames
//                    is MvStructLitField -> parent.parentStructLitExpr.providedFieldNames
        else -> emptySet()
    }
    return processor0.wrapWithFilter { e -> e.name !in providedFieldNames }
}