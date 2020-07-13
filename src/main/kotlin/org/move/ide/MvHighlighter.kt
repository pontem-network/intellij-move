package org.move.ide

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.move.lang.core.MV_KEYWORDS
import org.move.lang.MoveFlexAdapter
import org.move.lang.MoveTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

class MvHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = MoveFlexAdapter()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        val color = when (tokenType) {
            MoveTypes.NUMBER, MoveTypes.ADDRESS -> Default.NUMBER
            in MV_KEYWORDS -> Default.KEYWORD
            else -> null
        }
        return pack(color)
    }
}