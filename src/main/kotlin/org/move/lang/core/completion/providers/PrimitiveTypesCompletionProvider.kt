package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_ONLY_PRIMITIVE_TYPES
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.AngleBracketsInsertHandler
import org.move.lang.core.completion.PRIMITIVE_TYPE_PRIORITY
import org.move.lang.core.completion.withPriority
import org.move.lang.core.psi.ext.isMsl

object PrimitiveTypesCompletionProvider : MvCompletionProvider() {

    private var primitives: List<String> =
        PRIMITIVE_TYPE_IDENTIFIERS.toList() + BUILTIN_TYPE_IDENTIFIERS.toList()

    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            MvPsiPatterns.nameTypeIdentifier()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        if (parameters.position.parent.isMsl()) {
            primitives = primitives + SPEC_ONLY_PRIMITIVE_TYPES.toList()
        }
        primitives.forEach {
            val lookup = LookupElementBuilder.create(it).bold()
            val lookupString = lookup.lookupString

            val updatedLookup = when (lookupString) {
                "vector" -> lookup.withInsertHandler(AngleBracketsInsertHandler())
                "u16", "u32", "u256" -> lookup.withTypeText("sui only")
                else -> lookup
            }
            result.addElement(updatedLookup.withPriority(PRIMITIVE_TYPE_PRIORITY))
        }
    }
}
