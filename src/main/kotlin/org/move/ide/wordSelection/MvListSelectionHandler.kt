package org.move.ide.wordSelection

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.lang.core.LIST_CLOSE_SYMBOLS
import org.move.lang.core.LIST_OPEN_SYMBOLS
import org.move.lang.core.psi.MvBlockFields
import org.move.lang.core.psi.MvFunctionParameterList
import org.move.lang.core.psi.MvTupleFields
import org.move.lang.core.psi.MvTypeArgumentList
import org.move.lang.core.psi.MvTypeParameterList
import org.move.lang.core.psi.MvValueArgumentList

class MvListSelectionHandler : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement): Boolean =
        e is MvFunctionParameterList || e is MvValueArgumentList
                || e is MvTypeParameterList || e is MvTypeArgumentList
                || e is MvBlockFields || e is MvTupleFields

    override fun select(
        e: PsiElement,
        editorText: CharSequence,
        cursorOffset: Int,
        editor: Editor
    ): List<TextRange>? {
        val node = e.node ?: return null
        val startNode = node.findChildByType(LIST_OPEN_SYMBOLS) ?: return null
        val endNode = node.findChildByType(LIST_CLOSE_SYMBOLS) ?: return null
        val range = TextRange(startNode.startOffset + 1, endNode.startOffset)
        return listOf(range)
    }
}
