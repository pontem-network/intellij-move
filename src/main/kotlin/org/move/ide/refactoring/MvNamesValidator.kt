package org.move.ide.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.MvElementTypes.QUOTE_IDENTIFIER
import org.move.lang.core.MOVE_KEYWORDS
import org.move.lang.core.lexer.createMoveLexer

class MvNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?): Boolean = isKeyword(name)

    override fun isIdentifier(name: String, project: Project?): Boolean = isIdentifier(name)

    companion object {
        fun isIdentifier(name: String): Boolean = when (getLexerTokenType(name)) {
            QUOTE_IDENTIFIER -> true
            IDENTIFIER -> true
            else -> false
        }

        fun isKeyword(name: String) = getLexerTokenType(name) in MOVE_KEYWORDS
    }
}

fun isValidMoveVariableIdentifier(name: String): Boolean = getLexerTokenType(name) == IDENTIFIER

private fun getLexerTokenType(text: String): IElementType? {
    val lexer = createMoveLexer()
    lexer.start(text)
    return if (lexer.tokenEnd == text.length) lexer.tokenType else null
}
