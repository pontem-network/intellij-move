package org.move.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvMultiItemUse
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.startOffset
import org.move.lang.core.psi.psiFactory

class RemoveCurlyBracesIntention : MvElementBaseIntentionAction<RemoveCurlyBracesIntention.Context>() {
    override fun getText(): String = "Remove curly braces"
    override fun getFamilyName(): String = text

    data class Context(val itemUseGroup: MvMultiItemUse)

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val useStmt = element.ancestorStrict<MvUseStmt>() ?: return null
        val multiUse = useStmt.moduleItemUse?.multiItemUse ?: return null
        return Context(multiUse)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val itemUseGroup = ctx.itemUseGroup

        // Save the cursor position, adjusting for curly brace removal
        val caret = editor.caretModel.offset
        val newOffset = when {
            caret < itemUseGroup.startOffset -> caret
            caret < itemUseGroup.endOffset -> caret - 1
            else -> caret - 2
        }
        itemUseGroup.removeCurlyBraces()
        editor.caretModel.moveToOffset(newOffset)
    }
}

fun MvMultiItemUse.removeCurlyBraces() {
    val psiFactory = this.project.psiFactory
    val itemUse = this.itemUseList.singleOrNull() ?: return
    val refName = itemUse.referenceName
    val aliasName = itemUse.useAlias?.name

    var newText = refName
    if (aliasName != null) {
        newText += " as $aliasName"
    }
    val newItemUse = psiFactory.useItem(newText)
    this.replace(newItemUse)
}
