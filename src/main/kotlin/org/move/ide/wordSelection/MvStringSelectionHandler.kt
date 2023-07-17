package org.move.ide.wordSelection

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.BYTE_STRING_LITERAL
import org.move.lang.MvElementTypes.HEX_STRING_LITERAL
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.startOffset

class MvStringSelectionHandler : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement): Boolean =
        e.elementType == BYTE_STRING_LITERAL || e.elementType == HEX_STRING_LITERAL

    override fun select(
        e: PsiElement,
        editorText: CharSequence,
        cursorOffset: Int,
        editor: Editor
    ): List<TextRange>? {
        val startQuote = e.text.indexOf('"').takeIf { it != -1 } ?: return null
        val endQuote =
            e.text.indexOf('"', startIndex = startQuote + 1)
                .takeIf { it != -1 } ?: return null
        val range = TextRange(e.startOffset + startQuote + 1, e.startOffset + endQuote)
        return listOf(range)
    }
}
