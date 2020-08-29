package org.move.ide.refactoring

import com.intellij.psi.tree.IElementType
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.MoveLexer

//class MoveNamesValidator: NamesValidator {
//}

fun isValidMoveVariableIdentifier(name: String): Boolean = getLexerType(name) == IDENTIFIER

private fun getLexerType(text: String): IElementType? {
    val lexer = MoveLexer()
    lexer.start(text)
    return if (lexer.tokenEnd == text.length) lexer.tokenType else null
}
