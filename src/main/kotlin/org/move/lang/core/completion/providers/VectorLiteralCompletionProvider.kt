package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.MvElementTypes
import org.move.lang.MvElementTypes.COLON_COLON
import org.move.lang.core.MvPsiPattern
import org.move.lang.core.completion.VECTOR_LITERAL_PRIORITY
import org.move.lang.core.completion.withPriority
import org.move.lang.core.psi.MvPath

object VectorLiteralCompletionProvider : MvCompletionProvider() {

    override val elementPattern: ElementPattern<out PsiElement>
        get() = MvPsiPattern.pathExpr()
            .andNot(
                psiElement().afterLeaf(psiElement(COLON_COLON))
            )


    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val maybePath = parameters.position.parent
        val path = maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== path.referenceNameElement) return

        val lookupElement = LookupElementBuilder
            .create("vector[]")
            .withTypeText("vector<?>")
            .withInsertHandler { ctx, _ ->
                EditorModificationUtil.moveCaretRelatively(ctx.editor, -1)
            }
            .withPriority(VECTOR_LITERAL_PRIORITY)
        result.addElement(lookupElement)
    }

}
