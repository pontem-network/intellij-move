package org.move.ide.search

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import org.move.lang.MoveElementTypes.BYTE_STRING_LITERAL
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.MOVE_COMMENTS
import org.move.lang.core.lexer.createMoveLexer
import org.move.lang.core.tokenSetOf

class MoveWordsScanner : DefaultWordsScanner(
    createMoveLexer(),
    tokenSetOf(IDENTIFIER),
    MOVE_COMMENTS,
    tokenSetOf(BYTE_STRING_LITERAL)
) {
}
