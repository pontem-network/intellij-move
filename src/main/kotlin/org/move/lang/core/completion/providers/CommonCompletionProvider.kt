package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.VisibleForTesting
import org.move.lang.core.completion.Completions
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.getVerifiableItemEntries
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
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
        val completions = Completions(completionCtx, result)

        // handles dot expr
        if (element is MvMethodOrField) {
            addMethodOrFieldVariants(element, completions)
        }

        addCompletionVariants(element, completions)
    }

    fun addCompletionVariants(
        element: MvReferenceElement,
        completions: Completions,
    ) {
        when (element) {
            // `let Res/*caret*/ =`
            // catches all modules, enums, enum variants and struct patterns
            is MvPatBinding -> {
                var bindingEntries = getPatBindingsResolveVariants(element, true)
                    .dropExistingPatFields(element)
                    .dropInvisibleEntries(element)
                completions.addEntries(bindingEntries)
            }
            // `let Res { my_f/*caret*/: field }`
            is MvPatFieldFull -> {
                var entries = getStructPatFieldResolveVariants(element)
                    .dropExistingPatFields(element)
                    .dropInvisibleEntries(element)
                completions.addEntries(entries)
            }
            // loop labels
            is MvLabel -> {
                completions.addEntries(getLabelResolveVariants(element))
            }
            // `spec ITEM {}` module items, where ITEM is a reference to the function/struct/enum
            is MvItemSpecRef -> {
                completions.addEntries(getVerifiableItemEntries(element))
            }
        }
    }

    @VisibleForTesting
    fun addMethodOrFieldVariants(element: MvMethodOrField, completions: Completions) {
        val msl = completions.ctx.msl
        val receiverTy = element.inferReceiverTy(msl)
        // unknown, &unknown, &mut unknown
        if (receiverTy.unwrapRefs() is TyUnknown) return

        val tyAdt = receiverTy.unwrapRefs() as? TyAdt
        if (tyAdt != null) {
            val fieldEntries = getFieldLookupResolveVariants(element, tyAdt.item, msl)
            completions.addEntries(
                fieldEntries,
                applySubst = tyAdt.substitution
            )
        }

        val methodEntries = getMethodResolveVariants(element, receiverTy, msl)
        for (methodEntry in methodEntries) {
            val f = methodEntry.element() as? MvFunction ?: continue
            val subst = f.tyVarsSubst
            val funcTy = f.functionTy(msl).substitute(subst) as TyCallable
            val selfTy = funcTy.paramTypes.first()

            val autoborrowedReceiverTy =
                TyReference.autoborrow(receiverTy, selfTy)
                    ?: error("unreachable, references always compatible")
            val inferenceCtx = InferenceContext(msl)
            inferenceCtx.combineTypes(selfTy, autoborrowedReceiverTy)

            completions.addEntry(
                methodEntry,
                applySubst = inferenceCtx.resolveTypeVarsIfPossible(subst)
            )
        }
    }
}

private fun List<ScopeEntry>.dropExistingPatFields(refElement: MvReferenceElement): List<ScopeEntry> {
    // shorthand, skip all provided fields
    val parent = refElement.parent as? MvPatField ?: return this
    val existingFields = parent.patStruct.fieldNames
    return this
        .filter { it.name !in existingFields }
}
