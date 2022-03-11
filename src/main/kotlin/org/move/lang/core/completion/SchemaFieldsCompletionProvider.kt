package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.MvSchemaLitField
import org.move.lang.core.psi.ext.fieldNames
import org.move.lang.core.psi.ext.fieldStmts
import org.move.lang.core.psi.ext.schema
import org.move.lang.core.psi.ext.schemaLit
import org.move.lang.core.withParent

object SchemaFieldsCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns
            .psiElement()
            .withParent<MvSchemaLitField>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val pos = parameters.position
        val element = pos.parent as? MvSchemaLitField ?: return
        val schemaLit = element.schemaLit ?: return
        val schema = schemaLit.schema ?: return
        val providedFieldNames = schemaLit.fieldNames

        for (fieldStmt in schema.fieldStmts.filter { it.name !in providedFieldNames }) {
            result.addElement(fieldStmt.createCompletionLookupElement())
        }
    }
}
