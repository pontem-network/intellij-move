package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.withParent

object StructFieldsCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = StandardPatterns.or(
            PlatformPatterns
                .psiElement()
                .withParent<MvStructLitField>(),
            PlatformPatterns
                .psiElement()
                .withParent<MvPatField>(),
//            bindingPat()
//                .withSuperParent<MvPatField>(2),
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val pos = parameters.position
        var element = pos.parent as? MvElement ?: return

        if (element is MvPatBinding) element = element.parent as MvElement

        val completionCtx = MvCompletionContext(element, element.isMsl())
        when (element) {
            is MvPatField -> {
                val patStruct = element.parentPatStruct
                addFieldsToCompletion(
                    patStruct.path.maybeStruct ?: return,
                    patStruct.fieldNames,
                    result,
                    completionCtx
                )
            }
            is MvStructLitField -> {
                val structLit = element.parentStructLitExpr
                addFieldsToCompletion(
                    structLit.path.maybeStruct ?: return,
                    structLit.providedFieldNames,
                    result,
                    completionCtx
                )
            }
        }
    }


    private fun addFieldsToCompletion(
        referredStruct: MvStruct,
        providedFieldNames: Set<String>,
        result: CompletionResultSet,
        completionContext: MvCompletionContext,
    ) {
        for (field in referredStruct.namedFields.filter { it.name !in providedFieldNames }) {
            result.addElement(
                field.createLookupElement(completionContext)
            )
        }
    }
}

