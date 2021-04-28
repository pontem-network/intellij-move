package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.MoveStructPatField
import org.move.lang.core.psi.ext.fields
import org.move.lang.core.psi.ext.providedFieldNames
import org.move.lang.core.psi.ext.structLiteral
import org.move.lang.core.psi.ext.structPat
import org.move.lang.core.withParent

object StructFieldsCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = StandardPatterns.or(
            PlatformPatterns
                .psiElement()
                .withParent<MoveStructLiteralField>(),
            PlatformPatterns
                .psiElement()
                .withParent<MoveStructPatField>(),
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val pos = parameters.position
        val parent = pos.parent
        when (parent) {
            is MoveStructPatField -> {
                val structPat = parent.structPat
                addFieldsToCompletion(
                    structPat.referredStructDef ?: return,
                    structPat.providedFieldNames,
                    result
                )
            }
            is MoveStructLiteralField -> {
                val structLiteral = parent.structLiteral
                addFieldsToCompletion(
                    structLiteral.referredStructDef ?: return,
                    structLiteral.providedFieldNames,
                    result
                )
            }
        }
    }

    private fun addFieldsToCompletion(
        referredStruct: MoveStructDef,
        providedFieldNames: List<String>,
        result: CompletionResultSet,
    ) {
        for (field in referredStruct.fields.filter { it.name !in providedFieldNames }) {
            result.addElement(field.createLookupElement(false))
        }
    }
}
