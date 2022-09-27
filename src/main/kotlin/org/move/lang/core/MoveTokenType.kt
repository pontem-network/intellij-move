package org.move.lang.core

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.MoveLanguage
import org.move.lang.MoveParserDefinition.Companion.BLOCK_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT
import org.move.lang.MvElementTypes.*

class MvTokenType(debugName: String) : IElementType(debugName, MoveLanguage)

fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val MOVE_KEYWORDS = tokenSetOf(
    LET, MUT, ABORT, BREAK, CONTINUE, IF, ELSE, LOOP, RETURN, AS, WHILE,
    SCRIPT_KW, ADDRESS, MODULE_KW, PUBLIC, FUN, STRUCT_KW, ACQUIRES, USE, HAS, PHANTOM,
    MOVE, CONST_KW, NATIVE, FRIEND, ENTRY,
    ASSUME, ASSERT, REQUIRES, ENSURES, INVARIANT, MODIFIES, PRAGMA, INCLUDE, ABORTS_IF, WITH,
    SPEC, SCHEMA_KW, GLOBAL, LOCAL,
    EMITS, APPLY, TO, EXCEPT, INTERNAL,
    FORALL, EXISTS, IN, WHERE,
)

val TYPES = tokenSetOf(PATH_TYPE, REF_TYPE, TUPLE_TYPE)

val MOVE_COMMENTS = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT, EOL_DOC_COMMENT)

val MOVE_BINARY_OPS = tokenSetOf(
    OR_OR, AND_AND,
    EQ_EQ_GT, LT_EQ_EQ_GT,
    LT, LT_EQ, GT, GT_EQ,
    LT_LT, GT_GT,
    EQ_EQ, NOT_EQ,
    OR, AND, XOR,
    MUL, DIV, MODULO,
    PLUS, MINUS,
    XOR, AND, OR
)
