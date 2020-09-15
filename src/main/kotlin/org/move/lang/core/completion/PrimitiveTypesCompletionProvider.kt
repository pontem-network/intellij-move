package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.MoveQualifiedPathType
import org.move.lang.core.withCond
import org.move.lang.core.withSuperParent

object PrimitiveTypesCompletionProvider : MoveCompletionProvider() {

    private val primitives: List<String> =
        PRIMITIVE_TYPE_IDENTIFIERS.toList() + BUILTIN_TYPE_IDENTIFIERS.toList()

    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns.psiElement()
                .withSuperParent<MoveQualifiedPathType>(2)
                .withCond("FirstChild") { e -> e.prevSibling == null }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        primitives.forEach {
            result.addElement(LookupElementBuilder.create(it).bold().withPriority(PRIMITIVE_TYPE_PRIORITY))
        }
    }
}