package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.MvElementTypes
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvPath

object MacrosCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MvPsiPatterns.pathIdent()
            .andNot(MvPsiPatterns.pathType())
            .andNot(MvPsiPatterns.schemaRef())
            .andNot(
                PlatformPatterns.psiElement()
                    .afterLeaf(PlatformPatterns.psiElement(MvElementTypes.COLON_COLON))
            )


    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val maybePathIdent = parameters.position.parent
        val maybePath = maybePathIdent.parent
        val path = maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== path.referenceNameElement) return

        val lookupElement = LookupElementBuilder
            .create("assert!")
            .withTailText("(_: bool, err: u64)")
            .withTypeText("()")
            .withInsertHandler { ctx, _ ->
                val document = ctx.document
                document.insertString(ctx.selectionEndOffset, "()")
                EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
            }
        result.addElement(lookupElement)
    }

}
