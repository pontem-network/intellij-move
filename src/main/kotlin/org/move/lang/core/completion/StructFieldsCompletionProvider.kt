package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPatterns.bindingPat
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.withParent
import org.move.lang.core.withSuperParent

object StructFieldsCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = StandardPatterns.or(
            PlatformPatterns
                .psiElement()
                .withParent<MvStructLitField>(),
            PlatformPatterns
                .psiElement()
                .withParent<MvStructPatField>(),
            bindingPat()
                .withSuperParent<MvStructPatField>(2),
            PlatformPatterns
                .psiElement()
                .withParent<MvStructFieldRef>(),
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val pos = parameters.position
        var element = pos.parent
        if (element is MvBindingPat) element = element.parent
        when (element) {
            is MvStructPatField -> {
                val structPat = element.structPat
                addFieldsToCompletion(
                    structPat.path.maybeStruct ?: return,
                    structPat.fieldNames,
                    result
                )
            }
            is MvStructLitField -> {
                val structLit = element.structLit
                addFieldsToCompletion(
                    structLit.path.maybeStruct ?: return,
                    structLit.fieldNames,
                    result
                )
            }
            is MvStructFieldRef -> {
                processItems(element, Namespace.DOT_ACCESSED_FIELD) {
                    val field = it.element as? MvStructFieldDef
                    if (field != null) {
                        result.addElement(field.createLookupElement())
                    }
                    false
                }
            }
        }
    }

    private fun addFieldsToCompletion(
        referredStruct: MvStruct_,
        providedFieldNames: List<String>,
        result: CompletionResultSet,
    ) {
        for (field in referredStruct.fields.filter { it.name !in providedFieldNames }) {
            result.addElement(field.createLookupElement())
        }
    }
}
