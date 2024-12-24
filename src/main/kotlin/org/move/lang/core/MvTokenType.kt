package org.move.lang.core

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.MoveLanguage
import org.move.lang.MoveParserDefinition.Companion.BLOCK_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT
import org.move.lang.MvElementTypes.*

class MvTokenType(debugName: String): IElementType(debugName, MoveLanguage)

fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val CONTEXTUAL_KEYWORDS = tokenSetOf(ENUM_KW, MATCH_KW)
val MOVE_KEYWORDS = TokenSet.orSet(
    tokenSetOf(
        LET, MUT, ABORT, BREAK, CONTINUE, IF, ELSE, LOOP, RETURN, AS, WHILE, FOR,
        SCRIPT_KW, ADDRESS, MODULE_KW, PUBLIC, FUN, STRUCT_KW, ACQUIRES, USE, HAS, PHANTOM,
        MOVE, CONST_KW, NATIVE, FRIEND, ENTRY, INLINE,
        ASSUME, ASSERT, REQUIRES, ENSURES, INVARIANT, MODIFIES, PRAGMA, INCLUDE, ABORTS_IF, WITH, UPDATE, DECREASES,
        SPEC, SCHEMA_KW, GLOBAL, LOCAL,
        EMITS, APPLY, TO, EXCEPT, INTERNAL,
        FORALL, EXISTS, IN, WHERE, ENUM_KW, MATCH_KW,
    ),
//    CONTEXTUAL_KEYWORDS
)

val FUNCTION_MODIFIERS = tokenSetOf(VISIBILITY_MODIFIER, NATIVE, ENTRY, INLINE)
val TYPES = tokenSetOf(PATH_TYPE, REF_TYPE, TUPLE_TYPE)

val MV_COMMENTS = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT, EOL_DOC_COMMENT)

val MOVE_ARITHMETIC_BINARY_OPS = tokenSetOf(
    PLUS, MINUS, MUL, DIV, MODULO,
    AND, OR, XOR,
    LT_LT, GT_GT,
)
val MOVE_BINARY_OPS = tokenSetOf(
    OR_OR, AND_AND,
    EQ_EQ_GT, LT_EQ_EQ_GT,
    LT, LT_EQ, GT, GT_EQ,
    LT_LT, GT_GT,
    EQ_EQ, NOT_EQ,
    OR, AND, XOR,
    MUL, DIV, MODULO,
    PLUS, MINUS,
    XOR, AND, OR,
    PLUS_EQ, MINUS_EQ, MUL_EQ, DIV_EQ, MODULO_EQ,
    AND_EQ, OR_EQ, XOR_EQ, GT_GT_EQ, LT_LT_EQ,
)

val LIST_OPEN_SYMBOLS = tokenSetOf(L_PAREN, LT)
val LIST_CLOSE_SYMBOLS = tokenSetOf(R_PAREN, GT)
