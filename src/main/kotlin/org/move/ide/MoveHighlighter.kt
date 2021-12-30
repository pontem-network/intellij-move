package org.move.ide

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.move.ide.colors.MvColor
import org.move.lang.MvElementTypes.*
import org.move.lang.MoveParserDefinition.Companion.BLOCK_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT
import org.move.lang.core.MOVE_KEYWORDS
import org.move.lang.core.lexer.createMoveLexer

class MvHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = createMoveLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return pack(map(tokenType)?.textAttributesKey)
    }

    companion object {
        fun map(tokenType: IElementType): MvColor? = when (tokenType) {
            BLOCK_COMMENT -> MvColor.BLOCK_COMMENT
            EOL_COMMENT -> MvColor.EOL_COMMENT
            EOL_DOC_COMMENT -> MvColor.DOC_COMMENT

            L_PAREN, R_PAREN -> MvColor.PARENTHESES
            L_BRACE, R_BRACE -> MvColor.BRACES
            L_BRACK, R_BRACK -> MvColor.BRACKETS

            SEMICOLON -> MvColor.SEMICOLON
            DOT -> MvColor.DOT
            COMMA -> MvColor.COMMA

            BYTE_STRING_LITERAL, HEX_STRING_LITERAL -> MvColor.STRING
            INTEGER_LITERAL-> MvColor.NUMBER

            in MOVE_KEYWORDS, BOOL_LITERAL -> MvColor.KEYWORD
            else -> null
        }
    }
}
