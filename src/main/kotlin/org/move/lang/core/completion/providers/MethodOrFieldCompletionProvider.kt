package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.*
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.resolve.LetStmtScope.EXPR_STMT
import org.move.lang.core.resolve.LetStmtScope.NONE
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

        val msl = element.isMsl()
        val receiverTy = element.inferReceiverTy(msl).knownOrNull() ?: return
        val scopeInfo = ContextScopeInfo(
            letStmtScope = element.letStmtScope,
            refItemScopes = element.refItemScopes,
        )
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(element, msl)

        val ctx = CompletionContext(element, emptySet(), scopeInfo, expectedTy)

        val structItem = (receiverTy.derefIfNeeded() as? TyStruct)?.item
        if (structItem != null) {
            // add fields
            structItem.fields
                .forEach {
                    result.addElement(it.createLookupElementWithContext(ctx))
                }
        }
        getMethodVariants(element, receiverTy, msl)
            .forEach { (_, function) ->
                val lookupElement =
                    function.createLookupElementWithContext(
                        ctx,
                        insertHandler = object: DefaultInsertHandler(ctx) {
                            override fun handleInsert(context: InsertionContext, item: LookupElement) {
                                val psiFunction = item.psiElement as? MvFunction ?: return
                                val requiresExplicitTypeArguments =
                                    psiFunction.requiresExplicitlyProvidedTypeArguments(completionContext)
                                var suffix = ""
                                if (!context.hasAngleBrackets && requiresExplicitTypeArguments) {
                                    suffix += "<>"
                                }
                                if (!context.hasAngleBrackets && !context.hasCallParens) {
                                    suffix += "()"
                                }
                                val offset = when {
                                    requiresExplicitTypeArguments -> 1
                                    // dropping self parameter
                                    psiFunction.parameters.drop(1).isNotEmpty() -> 1
                                    else -> 2
                                }
                                context.document.insertString(context.selectionEndOffset, suffix)
                                EditorModificationUtil.moveCaretRelatively(context.editor, offset)
                            }
                        },
                        customModify = {
                            // overriding default for methods
                            withTailText(function.selfSignatureText)
                        })
                result.addElement(lookupElement)
            }
    }

}