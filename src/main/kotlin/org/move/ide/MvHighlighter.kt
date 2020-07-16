package org.move.ide

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.move.lang.core.MV_KEYWORDS
import org.move.lang.MvLexer
import org.move.lang.MvElementTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

class MvHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = MvLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        val color = when (tokenType) {
            MvElementTypes.BLOCK_COMMENT -> Default.BLOCK_COMMENT
            MvElementTypes.LINE_COMMENT -> Default.LINE_COMMENT
            MvElementTypes.HEX_STRING_LITERAL, MvElementTypes.BYTE_STRING_LITERAL -> Default.STRING
            MvElementTypes.INTEGER_LITERAL, MvElementTypes.ADDRESS_LITERAL -> Default.NUMBER
            MvElementTypes.BOOL_LITERAL -> Default.KEYWORD
            in MV_KEYWORDS -> Default.KEYWORD
            else -> null
        }
        return pack(color)
    }
}