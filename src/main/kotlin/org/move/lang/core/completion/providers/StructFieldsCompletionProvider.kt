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
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve2.ref.FieldResolveVariant
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
                val patStruct = element.patStruct
                // Path resolution is cached, but sometimes path changes so much that it can't be retrieved
                // from cache anymore. In this case we need to get the old path.
                // OLD: "safe" here means that if tree changes too much (=any of the ancestors of path are changed),
                // then it's a no-op and we continue working with current path.
                val struct = patStruct.path.getOriginalOrSelf().maybeFieldsOwner ?: return
                addFieldsToCompletion(
                    struct,
                    patStruct.fieldNames,
                    result,
                    completionCtx
                )
            }
            is MvStructLitField -> {
                val structLit = element.parentStructLitExpr
                // see MvPatField's comment above
                val struct = structLit.path.getOriginalOrSelf().maybeFieldsOwner ?: return
                addFieldsToCompletion(
                    struct,
                    structLit.providedFieldNames,
                    result,
                    completionCtx
                )
            }
        }
    }


    private fun addFieldsToCompletion(
        fieldsOwner: MvFieldsOwner,
        providedFieldNames: Set<String>,
        result: CompletionResultSet,
        completionContext: MvCompletionContext,
    ) {
        for (field in fieldsOwner.namedFields.filter { it.name !in providedFieldNames }) {
            val scopeEntry = FieldResolveVariant(field.name, field)
            createLookupElement(scopeEntry, completionContext)
            result.addElement(
                createLookupElement(scopeEntry, completionContext)
            )
        }
    }
}

