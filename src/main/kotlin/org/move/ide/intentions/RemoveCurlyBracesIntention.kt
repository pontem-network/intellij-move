package org.move.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.startOffset
import org.move.lang.core.psi.psiFactory

class RemoveCurlyBracesIntention: MvElementBaseIntentionAction<RemoveCurlyBracesIntention.Context>() {
    override fun getText(): String = "Remove curly braces"
    override fun getFamilyName(): String = text

    data class Context(val useSpeck: MvUseSpeck)

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val useStmt = element.ancestorStrict<MvUseStmt>() ?: return null
        val useSpeck = useStmt.useSpeck ?: return null
        val useGroup = useSpeck.useGroup ?: return null
        if (useGroup.useSpeckList.size > 1) return null
        return Context(useSpeck)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val useSpeck = ctx.useSpeck

        // Save the cursor position, adjusting for curly brace removal
        val caret = editor.caretModel.offset
        val newOffset = when {
            caret < useSpeck.startOffset -> caret
            caret < useSpeck.endOffset -> caret - 1
            else -> caret - 2
        }
        useSpeck.removeCurlyBraces()
        editor.caretModel.moveToOffset(newOffset)
    }
}

private fun MvUseSpeck.removeCurlyBraces() {
    val psiFactory = this.project.psiFactory
    val useGroup = this.useGroup ?: return
    val itemUseSpeck = useGroup.useSpeckList.singleOrNull() ?: return

    val newPath = psiFactory.path("0x1::dummy::call")

    val itemIdentifier = itemUseSpeck.path.identifier ?: return
    // copy identifier
    newPath.identifier?.replace(itemIdentifier.copy())
    // copy module path
    newPath.path?.replace(this.path.copy())

    val dummyUseSpeck = psiFactory.useSpeck("0x1::dummy::call as mycall")
    dummyUseSpeck.path.replace(newPath)

    val useAlias = itemUseSpeck.useAlias
    if (useAlias != null) {
        dummyUseSpeck.useAlias?.replace(useAlias)
    } else {
        dummyUseSpeck.useAlias?.delete()
    }


//    val aliasName = itemUseSpeck.useAlias?.name
//
//    var newText = refName
//    if (aliasName != null) {
//        newText += " as $aliasName"
//    }
//    val newItemUse = psiFactory.useSpeckForGroup(newText)
    this.replace(dummyUseSpeck)
}
