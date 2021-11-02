package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
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
            PlatformPatterns
                .psiElement()
                .withParent<MoveStructFieldRef>(),
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val pos = parameters.position
        val element = pos.parent
        when (element) {
            is MoveStructPatField -> {
                val structPat = element.structPat
                addFieldsToCompletion(
                    structPat.path.maybeStruct ?: return,
                    structPat.providedFieldNames,
                    result
                )
            }
            is MoveStructLiteralField -> {
                val structLiteral = element.structLiteral
                addFieldsToCompletion(
                    structLiteral.path.maybeStruct ?: return,
                    structLiteral.providedFieldNames,
                    result
                )
            }
            is MoveStructFieldRef -> {
                processItems(element, Namespace.DOT_ACCESSED_FIELD) {
                    val field = it.element as? MoveStructFieldDef
                    if (field != null) {
                        result.addElement(field.createLookupElement(false))
                    }
                    false
                }
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
