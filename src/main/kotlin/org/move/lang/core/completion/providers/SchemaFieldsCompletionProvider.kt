package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.MvSchemaLitField
import org.move.lang.core.psi.ext.fields
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.processSchemaLitFieldResolveVariants
import org.move.lang.core.psi.ext.schemaLit
import org.move.lang.core.resolve.collectCompletionVariants
import org.move.lang.core.resolve.wrapWithFilter
import org.move.lang.core.withParent

object SchemaFieldsCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns.psiElement().withParent<MvSchemaLitField>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val pos = parameters.position
        val literalField = pos.parent as? MvSchemaLitField ?: return

        val schemaLit = literalField.schemaLit?.getOriginalOrSelf() ?: return
        val existingFieldNames = schemaLit.fields
            .filter { !it.textRange.contains(pos.textOffset) }
            .map { it.referenceName }

        val completionCtx = MvCompletionContext(literalField, literalField.isMsl())
        collectCompletionVariants(result, completionCtx) {
            val processor = it.wrapWithFilter { e -> e.name !in existingFieldNames }
            processSchemaLitFieldResolveVariants(literalField, processor)
        }
    }
}
