package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPattern.bindingPat
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.withParent
import org.move.lang.core.withSuperParent

object StructFieldsCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = StandardPatterns.or(
            PlatformPatterns
                .psiElement()
                .withParent<MvStructLitField>(),
            PlatformPatterns
                .psiElement()
                .withParent<MvFieldPat>(),
            bindingPat()
                .withSuperParent<MvFieldPat>(2),
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val pos = parameters.position
        var element = pos.parent as? MvElement ?: return

        if (element is MvBindingPat) element = element.parent as MvElement

        val completionCtx = CompletionContext(element, element.isMsl())
//        val completionCtx = CompletionContext(element, ContextScopeInfo.default())
        when (element) {
            is MvFieldPat -> {
                val structPat = element.structPat
                addFieldsToCompletion(
                    structPat.path.maybeStruct ?: return,
                    structPat.patFieldNames,
                    result,
                    completionCtx
                )
            }
            is MvStructLitField -> {
                val structLit = element.structLitExpr
                addFieldsToCompletion(
                    structLit.path.maybeStruct ?: return,
                    structLit.fieldNames,
                    result,
                    completionCtx
                )
            }
        }
    }


    private fun addFieldsToCompletion(
        referredStruct: MvStruct,
        providedFieldNames: List<String>,
        result: CompletionResultSet,
        completionContext: CompletionContext,
    ) {
        for (field in referredStruct.fields.filter { it.name !in providedFieldNames }) {
            result.addElement(
                field.createLookupElement(completionContext)
            )
        }
    }
}

