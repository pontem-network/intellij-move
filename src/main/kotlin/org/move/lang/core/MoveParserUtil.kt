package org.move.lang.core

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderUtil
import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.BitUtil
import org.move.lang.MoveParserDefinition.Companion.EOL_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT
import org.move.lang.MvElementTypes.*
import org.move.stdext.makeBitMask

enum class FunModifier {
    VIS, NATIVE, ENTRY, INLINE;
}

@Suppress("UNUSED_PARAMETER")
object MoveParserUtil : GeneratedParserUtilBase() {
    @JvmField
    val ADJACENT_LINE_COMMENTS = WhitespacesAndCommentsBinder { tokens, _, getter ->
        var candidate = tokens.size
        for (i in 0 until tokens.size) {
            val token = tokens[i]
            if (EOL_DOC_COMMENT == token) {
                candidate = minOf(candidate, i)
                break
            }
            if (EOL_COMMENT == token) {
                candidate = minOf(candidate, i)
            }
            if (WHITE_SPACE == token && "\n\n" in getter[i]) {
                candidate = tokens.size
            }
        }
        candidate
    }

    @JvmStatic
    fun gtgtImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GT_GT, GT, GT)

    @JvmStatic
    fun gteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GT_EQ, GT, EQ)

    @JvmStatic
    fun ltltImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, LT_LT, LT, LT)

    @JvmStatic
    fun lteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, LT_EQ, LT, EQ)

    @JvmStatic
    fun ororImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, OR_OR, OR, OR)

    @JvmStatic
    fun andandImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, AND_AND, AND, AND)

    @JvmStatic
    fun eqeqgtImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, EQ_EQ_GT, EQ_EQ, GT)

    @JvmStatic
    fun lteqeqgtImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, LT_EQ_EQ_GT, LT, EQ_EQ, GT)

    @JvmStatic
    private fun collapse(b: PsiBuilder, tokenType: IElementType, vararg parts: IElementType): Boolean {
        // We do not want whitespace between parts, so firstly we do raw lookup for each part,
        // and when we make sure that we have desired token, we consume and collapse it.
        parts.forEachIndexed { i, tt ->
            if (b.rawLookup(i) != tt) return false
        }
        val marker = b.mark()
        val expectedLength = parts.size
        PsiBuilderUtil.advance(b, expectedLength)
        marker.collapse(tokenType)
        return true
    }

    @JvmStatic
    fun hexIntegerLiteral(b: PsiBuilder, level: Int): Boolean {
        if (b.tokenType == HEX_INTEGER_LITERAL) {
            b.advanceLexer()
            return true
        }
        if (b.tokenType != DIEM_ADDRESS) return false
        // should not be :: next, it's an address then
        if (b.lookAhead(1) == COLON_COLON) return false

        b.remapCurrentToken(HEX_INTEGER_LITERAL)
        b.advanceLexer()
        return true
    }

    @JvmStatic
    fun patternIdent(b: PsiBuilder, level: Int): Boolean {
        if (b.tokenType == FUNCTION_PATTERN_IDENT) {
            b.advanceLexer(); return true; }
        if (b.tokenType in tokenSetOf(MUL, IDENTIFIER)) {
            val marker = b.mark()
            while (true) {
                val consumed = consumeStopOnWhitespaceOrComment(b, tokenSetOf(MUL, IDENTIFIER))
                if (!consumed) break
            }
            marker.collapse(FUNCTION_PATTERN_IDENT)
            return true
        }
        return false
    }

    @JvmStatic
    private fun consumeStopOnWhitespaceOrComment(b: PsiBuilder, tokens: TokenSet): Boolean {
        val nextTokenType = b.rawLookup(1)
        if (nextTokenType in MOVE_COMMENTS || nextTokenType == WHITE_SPACE) {
            consumeToken(b, tokens)
            return false
        }
        return consumeToken(b, tokens)
    }

    @JvmStatic
    fun patternVisibility(b: PsiBuilder, level: Int): Boolean {
        if (b.tokenType in tokenSetOf(PUBLIC, INTERNAL)) {
            b.advanceLexer()
            return true
        }
        if (b.tokenType == IDENTIFIER && b.tokenText == "internal") {
            b.remapCurrentToken(INTERNAL)
            b.advanceLexer()
            return true
        }
        return false
    }

    @JvmStatic
    fun msl(b: PsiBuilder, level: Int, parser: Parser): Boolean {
        b.mslLevel = b.mslLevel + 1
        val result = parser.parse(b, level)
        b.mslLevel = b.mslLevel - 1
        return result
    }

    @JvmStatic
    fun mslOnly(b: PsiBuilder, level: Int, parser: Parser): Boolean {
        if (b.mslLevel == 0) return false
        return parser.parse(b, level)
    }

    @JvmStatic
    fun includeStmtMode(b: PsiBuilder, level: Int, parser: Parser): Boolean {
        val oldFlags = b.flags
        val newFlags = oldFlags
            .setFlag(INCLUDE_STMT_MODE, true)
        b.flags = newFlags
        val result = parser.parse(b, level)
        b.flags = oldFlags
        return result
    }

    @JvmStatic
    fun includeStmtModeFalse(b: PsiBuilder, level: Int): Boolean = !includeStmtModeTrue(b, level)

    @JvmStatic
    fun includeStmtModeTrue(b: PsiBuilder, level: Int): Boolean = BitUtil.isSet(b.flags, INCLUDE_STMT_MODE)

    @JvmStatic
    fun functionModifierSet(
        b: PsiBuilder,
        level: Int,
        visParser: Parser,
    ): Boolean {
        return innerFunctionModifierSet(
            b,
            level,
            visParser,
            native = false
        )
    }

    @JvmStatic
    fun nativeFunctionModifierSet(
        b: PsiBuilder,
        level: Int,
        visParser: Parser,
    ): Boolean {
        return innerFunctionModifierSet(
            b,
            level,
            visParser,
            native = true
        )
    }

    private fun innerFunctionModifierSet(
        b: PsiBuilder,
        level: Int,
        visParser: Parser,
        native: Boolean,
    ): Boolean {
        val modifiersLeft = FunModifier.values().toMutableSet()
        if (!native) {
            modifiersLeft.remove(FunModifier.NATIVE)
        }
        var nativeEncountered = false
        var parsed = false

        fun isParsed() = parsed && if (native) nativeEncountered else true

        while (modifiersLeft.isNotEmpty()) {
            when {
                b.tokenType == PUBLIC -> {
                    if (FunModifier.VIS !in modifiersLeft) return isParsed()
                    if (!visParser.parse(b, level)) return false
                    modifiersLeft.remove(FunModifier.VIS)
                    parsed = true
                }
                b.tokenType == NATIVE -> {
                    if (FunModifier.NATIVE !in modifiersLeft) return isParsed()
                    modifiersLeft.remove(FunModifier.NATIVE)
                    // native alone only should give true for next token fun
                    parsed = parsed || (b.lookAhead(1) == FUN)
                    nativeEncountered = true
                    b.advanceLexer()
                }
                entryKeyword(b, level) -> {
                    if (FunModifier.ENTRY !in modifiersLeft) return isParsed()
                    modifiersLeft.remove(FunModifier.ENTRY)
                    parsed = true
                }
                inlineKeyword(b, level) -> {
                    if (FunModifier.INLINE !in modifiersLeft) return isParsed()
                    modifiersLeft.remove(FunModifier.INLINE)
                    parsed = true
                }
                else -> return isParsed()
            }

        }
        return isParsed()

    }

    @JvmStatic
    fun invariantModifierKeyword(b: PsiBuilder, level: Int): Boolean {
        if (b.tokenType in tokenSetOf(PACK, UNPACK, UPDATE)) {
            b.advanceLexer()
            return true
        }
        if (b.tokenType != IDENTIFIER) return false

        val tokenType = when (b.tokenText) {
            "pack" -> PACK
            "unpack" -> UNPACK
            "update" -> UPDATE
            else -> {
                return false
            }
        }
        b.remapCurrentToken(tokenType)
        b.advanceLexer()
        return true
    }

    @Suppress("FunctionName")
    @JvmStatic
    fun VECTOR_IDENTIFIER(b: PsiBuilder, level: Int): Boolean {
        return nextTokenIs(b, "vector") && consumeToken(b, IDENTIFIER)
    }

    @JvmStatic
    fun addressKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "address", ADDRESS)

    @JvmStatic
    fun hasKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "has", HAS)

    @JvmStatic
    fun entryKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "entry", ENTRY)

    @JvmStatic
    fun inlineKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "inline", INLINE)

    @JvmStatic
    fun schemaKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "schema", SCHEMA_KW)

//    @JvmStatic
//    fun vectorKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "vector", VECTOR_KW)

    @JvmStatic
    fun friendKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "friend", FRIEND)

    @JvmStatic
    fun pragmaKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "pragma", PRAGMA)

    @JvmStatic
    fun postKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "post", POST)

    @JvmStatic
    fun localKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "local", LOCAL)

    @JvmStatic
    fun globalKeyword(b: PsiBuilder, level: Int): Boolean =
        contextualKeyword(b, "global", GLOBAL, { it !in CALL_EXPR_START_TOKENS })

    private val CALL_EXPR_START_TOKENS = tokenSetOf(L_PAREN, LT)

    @JvmStatic
    fun forallKeyword(b: PsiBuilder, level: Int): Boolean =
        contextualKeyword(b, "forall", FORALL, { it !in CALL_EXPR_START_TOKENS })

    @JvmStatic
    fun existsKeyword(b: PsiBuilder, level: Int): Boolean =
        contextualKeyword(b, "exists", EXISTS, { it !in CALL_EXPR_START_TOKENS })

    @JvmStatic
    fun withKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "with", WITH)

    @JvmStatic
    fun whereKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "where", WHERE)

    @JvmStatic
    fun inKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "in", IN)

    @JvmStatic
    fun includeKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "include", INCLUDE)

    @JvmStatic
    fun chooseKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "choose", CHOOSE)

    @JvmStatic
    fun minKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "min", MIN)

    @JvmStatic
    fun invariantKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "invariant", INVARIANT)

    @JvmStatic
    fun axiomKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "axiom", AXIOM)

    @JvmStatic
    fun abortsIfKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "aborts_if", ABORTS_IF)

    @JvmStatic
    fun abortsWithKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "aborts_with", ABORTS_WITH)

    @JvmStatic
    fun assertKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "assert", ASSERT)

    @JvmStatic
    fun assumeKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "assume", ASSUME)

    @JvmStatic
    fun modifiesKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "modifies", MODIFIES)

    @JvmStatic
    fun ensuresKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "ensures", ENSURES)

    @JvmStatic
    fun requiresKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "requires", REQUIRES)

    @JvmStatic
    fun emitsKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "emits", EMITS)

    @JvmStatic
    fun applyKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "apply", APPLY)

    @JvmStatic
    fun exceptKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "except", EXCEPT)

    @JvmStatic
    fun toKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "to", TO)

    private val FLAGS: Key<Int> = Key("MoveParserUtil.FLAGS")
    private var PsiBuilder.flags: Int
        get() = getUserData(FLAGS) ?: TOP_LEVEL
        set(value) = putUserData(FLAGS, value)

    private fun Int.setFlag(flag: Int, mode: Boolean): Int =
        BitUtil.set(this, flag, mode)

    // flags
    private val TOP_LEVEL: Int = makeBitMask(0)
    private val INCLUDE_STMT_MODE: Int = makeBitMask(1)

    // msl
    private val MSL_LEVEL: Key<Int> = Key("MoveParserUtil.MSL_LEVEL")
    private var PsiBuilder.mslLevel: Int
        get() = getUserData(MSL_LEVEL) ?: 0
        set(value) = putUserData(MSL_LEVEL, value)

    private fun contextualKeyword(
        b: PsiBuilder,
        keyword: String,
        elementType: IElementType,
        nextElementPredicate: (IElementType?) -> Boolean = { it !in tokenSetOf() }
    ): Boolean {
        if (b.tokenType == elementType ||
            b.tokenType == IDENTIFIER && b.tokenText == keyword && nextElementPredicate(b.lookAhead(1))
        ) {
            b.remapCurrentToken(elementType)
            b.advanceLexer()
            return true
        }
        return false
    }

}
