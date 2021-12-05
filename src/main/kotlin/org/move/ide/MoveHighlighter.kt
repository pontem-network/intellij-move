package org.move.ide

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.move.ide.colors.MoveColor
import org.move.lang.MoveElementTypes.*
import org.move.lang.MoveParserDefinition.Companion.BLOCK_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT
import org.move.lang.core.MOVE_KEYWORDS
import org.move.lang.core.lexer.createMoveLexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

class MoveHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = createMoveLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return pack(map(tokenType)?.textAttributesKey)
    }

    companion object {
        fun map(tokenType: IElementType): MoveColor? = when (tokenType) {
            BLOCK_COMMENT -> MoveColor.BLOCK_COMMENT
            EOL_COMMENT -> MoveColor.EOL_COMMENT
            EOL_DOC_COMMENT -> MoveColor.DOC_COMMENT

            L_PAREN, R_PAREN -> MoveColor.PARENTHESES
            L_BRACE, R_BRACE -> MoveColor.BRACES
            L_BRACK, R_BRACK -> MoveColor.BRACKETS

            SEMICOLON -> MoveColor.SEMICOLON
            DOT -> MoveColor.DOT
            COMMA -> MoveColor.COMMA

            BYTE_STRING_LITERAL, HEX_STRING_LITERAL -> MoveColor.STRING
            INTEGER_LITERAL, ADDRESS_LITERAL -> MoveColor.NUMBER

            in MOVE_KEYWORDS, BOOL_LITERAL -> MoveColor.KEYWORD
            else -> null
        }
    }
}
