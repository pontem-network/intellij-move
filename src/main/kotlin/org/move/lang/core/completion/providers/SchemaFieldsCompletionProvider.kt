package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvSchemaLitField
import org.move.lang.core.psi.completionPriority
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.createProcessor
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

        val completionCtx = CompletionContext(literalField, literalField.isMsl())
        val completionCollector = createProcessor { e ->
            val element = e.element as? MvNamedElement ?: return@createProcessor
            // check for visibility
//            if (!e.isVisibleFrom(pathElement)) return@createProcessor
            if (e.name in existingFieldNames) return@createProcessor
            val lookup =
                element.createLookupElement(
                    completionCtx,
                    priority = element.completionPriority
                )
            result.addElement(lookup)
        }
        processSchemaLitFieldResolveVariants(literalField, completionCollector)

//        val completionCtx = CompletionContext(element, ContextScopeInfo.msl())
//        for (fieldBinding in schema.fieldBindings.filter { it.name !in providedFieldNames }) {
//            result.addElement(
//                fieldBinding.createLookupElement(completionCtx)
//            )
//        }
    }
}
