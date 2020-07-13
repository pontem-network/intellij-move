package org.move.lang.core

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.MoveLanguage
import org.move.lang.MoveTypes.*

class MvTokenType(debugName: String) : IElementType(debugName, MoveLanguage)

fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val MV_KEYWORDS = tokenSetOf(
    LET, MUT, ABORT, BREAK, CONTINUE, IF, ELSE, LOOP, RETURN, AS,
    SCRIPT, ADDRESS, MODULE, PUBLIC, FUN, STRUCT,
    BOOL_TRUE, BOOL_FALSE
)