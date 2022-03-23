package org.move.lang.core.completion

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
import org.move.lang.core.psi.MvElement
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
            var lookup = LookupElementBuilder.create(it).bold()
            if (lookup.lookupString == "vector") {
                lookup = lookup.withInsertHandler(AngleBracketsInsertHandler())
            }
            result.addElement(lookup.withPriority(PRIMITIVE_TYPE_PRIORITY))
        }
    }
}
