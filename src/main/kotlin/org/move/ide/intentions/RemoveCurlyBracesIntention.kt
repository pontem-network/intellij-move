package org.move.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvImportStatement
import org.move.lang.core.psi.MvMultiItemImport
import org.move.lang.core.psi.MvPsiFactory
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.startOffset

class RemoveCurlyBracesIntention: MvElementBaseIntentionAction<RemoveCurlyBracesIntention.Context>() {
    override fun getText(): String = "Remove curly braces"
    override fun getFamilyName(): String = text

    data class Context(
        val multiItemImport: MvMultiItemImport,
        val refName: String,
        val aliasName: String?
    )

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val importStatement =
            element.ancestorStrict<MvImportStatement>() ?: return null
        val multiItemImport =
            importStatement.moduleItemsImport?.multiItemImport ?: return null
        val itemImport = multiItemImport.itemImportList.singleOrNull() ?: return null
        val refName = itemImport.referenceName
        val aliasName = itemImport.importAlias?.name
        return Context(multiItemImport, refName, aliasName)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val (multiItemImport, refName, aliasName) = ctx
        // Save the cursor position, adjusting for curly brace removal
        val caret = editor.caretModel.offset
        val newOffset = when {
            caret < multiItemImport.startOffset -> caret
            caret < multiItemImport.endOffset -> caret - 1
            else -> caret - 2
        }

        var newText = refName
        if (aliasName != null) {
            newText += " as $aliasName"
        }
        val newItemImport = MvPsiFactory(project).createItemImport(newText)
        multiItemImport.replace(newItemImport)

        editor.caretModel.moveToOffset(newOffset)
    }
}
