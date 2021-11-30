package org.move.lang.core

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.MoveLanguage
import org.move.lang.MoveElementTypes.*
import org.move.lang.MoveParserDefinition.Companion.BLOCK_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT

class MoveTokenType(debugName: String) : IElementType(debugName, MoveLanguage)

fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val MOVE_KEYWORDS = tokenSetOf(
    LET, MUT, ABORT, BREAK, CONTINUE, IF, ELSE, LOOP, RETURN, AS, WHILE,
    SCRIPT, ADDRESS, MODULE, PUBLIC, FUN, STRUCT, ACQUIRES, USE, HAS, PHANTOM,
    MOVE, CONST, NATIVE, FRIEND,
    ASSUME, ASSERT, REQUIRES, ENSURES, INVARIANT, MODIFIES, PRAGMA, INCLUDE, ABORTS_IF, WITH,
    SPEC, SCHEMA, GLOBAL, LOCAL,
    EMITS, APPLY, TO, EXCEPT, INTERNAL,
    FORALL, EXISTS, IN, WHERE,
)

val MOVE_COMMENTS = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT, EOL_DOC_COMMENT)
