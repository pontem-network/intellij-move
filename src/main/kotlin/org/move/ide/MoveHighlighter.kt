package org.move.ide

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.move.lang.MoveElementTypes.*
import org.move.lang.MoveParserDefinition.Companion.BLOCK_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT
import org.move.lang.core.MOVE_KEYWORDS
import org.move.lang.core.lexer.createMoveLexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

class MoveHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = createMoveLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        val color = when (tokenType) {
            BLOCK_COMMENT -> Default.BLOCK_COMMENT
            EOL_COMMENT, EOL_DOC_COMMENT -> Default.LINE_COMMENT

            L_PAREN, R_PAREN -> Default.PARENTHESES
            L_BRACE, R_BRACE -> Default.BRACES
            L_BRACK, R_BRACK -> Default.BRACKETS

            SEMICOLON -> Default.SEMICOLON
            DOT -> Default.DOT
            COMMA -> Default.COMMA

            BYTE_STRING_LITERAL -> Default.STRING
            INTEGER_LITERAL, ADDRESS_LITERAL -> Default.NUMBER

            in MOVE_KEYWORDS, BOOL_LITERAL -> Default.KEYWORD
            IDENTIFIER -> Default.IDENTIFIER

            else -> null
        }
        return pack(color)
    }
}
