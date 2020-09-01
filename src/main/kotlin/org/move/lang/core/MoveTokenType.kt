package org.move.lang.core

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.MoveLanguage
import org.move.lang.MoveElementTypes.*

class MoveTokenType(debugName: String) : IElementType(debugName, MoveLanguage)

fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val MV_KEYWORDS = tokenSetOf(
    LET, MUT, ABORT, BREAK, CONTINUE, IF, ELSE, LOOP, RETURN, AS, WHILE,
    SCRIPT, ADDRESS, MODULE, PUBLIC, FUN, STRUCT, ACQUIRES, USE, RESOURCE, COPYABLE,
    COPY, MOVE, CONST, NATIVE,
    ASSUME, ASSERT, REQUIRES, ENSURES, INVARIANT, MODIFIES, PRAGMA, INCLUDE, ABORTS_IF, WITH, PACK, UNPACK,
    SPEC, DEFINE, SCHEMA, GLOBAL, LOCAL,
    APPLY, TO, EXCEPT,
    UPDATE, FORALL, EXISTS, IN,
)

val MV_COMMENTS = tokenSetOf(BLOCK_COMMENT, LINE_COMMENT)