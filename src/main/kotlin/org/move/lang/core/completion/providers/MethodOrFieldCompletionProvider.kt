package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.VisibleForTesting
import org.move.lang.core.completion.*
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.resolve.letStmtScope
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
        val scopeInfo = ContextScopeInfo(
            letStmtScope = element.letStmtScope,
            refItemScopes = element.refItemScopes,
        )
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(element, msl)

        val ctx = CompletionContext(element, scopeInfo, expectedTy)

        val structTy = receiverTy.derefIfNeeded() as? TyStruct
        if (structTy != null) {
            getFieldVariants(element, structTy, msl)
                .forEach { (_, field) ->
                    result.addElement(field.createLookupElement(ctx))
                }
        }
        getMethodVariants(element, receiverTy, msl)
            .forEach { (_, function) ->
                val lookupElement = function.createLookupElement(ctx)
//                val lookupProperties = lookupProperties(function, context = ctx)
//                val builder = lookupElement
//                    .withTailText("")
//                    .withTypeText("")
//                    .withInsertHandler(DefaultInsertHandler())
//                val mvLookupElement = builder.withPriority(DEFAULT_PRIORITY).toMvLookupElement(lookupProperties)
                result.addElement(lookupElement)
            }
    }

}