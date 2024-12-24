package org.move.ide.search

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import org.move.lang.MvElementTypes.BYTE_STRING_LITERAL
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.MV_COMMENTS
import org.move.lang.core.lexer.createMoveLexer
import org.move.lang.core.tokenSetOf

class MvWordsScanner : DefaultWordsScanner(
    createMoveLexer(),
    tokenSetOf(IDENTIFIER),
    MV_COMMENTS,
    tokenSetOf(BYTE_STRING_LITERAL)
)
