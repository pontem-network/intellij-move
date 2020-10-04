package org.move.ide.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.MOVE_KEYWORDS
import org.move.lang.core.lexer.createMoveLexer

class MoveNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?): Boolean {
        return getLexerType(name) in MOVE_KEYWORDS
    }

    override fun isIdentifier(name: String, project: Project?): Boolean = isIdentifier(name)

    companion object {
        fun isIdentifier(name: String): Boolean = when (getLexerType(name)) {
            IDENTIFIER -> true
            else -> false
        }
    }
}

fun isValidMoveVariableIdentifier(name: String): Boolean = getLexerType(name) == IDENTIFIER

private fun getLexerType(text: String): IElementType? {
    val lexer = createMoveLexer()
    lexer.start(text)
    return if (lexer.tokenEnd == text.length) lexer.tokenType else null
}
