package org.move.ide.typing

import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.move.lang.MvElementTypes.BYTE_STRING_LITERAL
import org.move.lang.MvElementTypes.HEX_STRING_LITERAL

class MvQuoteHandler : SimpleTokenSetQuoteHandler(BYTE_STRING_LITERAL, HEX_STRING_LITERAL),
                       MultiCharQuoteHandler {

    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        val elementType = iterator.tokenType
        val start = iterator.start
        val result = when (elementType) {
            BYTE_STRING_LITERAL, HEX_STRING_LITERAL -> start == (offset + 1)
            else -> super.isOpeningQuote(iterator, offset)
        }
        return result
    }

    override fun isNonClosedLiteral(iterator: HighlighterIterator, chars: CharSequence): Boolean {
        if (iterator.tokenType == HEX_STRING_LITERAL
            || iterator.tokenType == BYTE_STRING_LITERAL
        ) {
            return (iterator.end - iterator.start) == 2
        }

        return super.isNonClosedLiteral(iterator, chars)
    }

    override fun getClosingQuote(iterator: HighlighterIterator, offset: Int) = "\""

}
