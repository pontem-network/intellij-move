package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.Completions
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvPatField
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.scopeEntry.asEntries
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
        val completions = Completions(completionCtx, result)

        when (element) {
            is MvPatField -> {
                val patStruct = element.patStruct
                val struct = patStruct.path.maybeFieldsOwner ?: return
                val existingFields = patStruct.fieldNames.toSet()
                val fieldEntries =
                    struct.namedFields.filter { it.name !in existingFields }.asEntries()
                completions.addEntries(fieldEntries)
            }
            is MvStructLitField -> {
                val structLit = element.parentStructLitExpr
                val struct = structLit.path.maybeFieldsOwner ?: return
                val existingFields = structLit.providedFieldNames.toSet()
                val fieldEntries =
                    struct.namedFields.filter { it.name !in existingFields }.asEntries()
                completions.addEntries(fieldEntries)
            }
        }
    }
}

