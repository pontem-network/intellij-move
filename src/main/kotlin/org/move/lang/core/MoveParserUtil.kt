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
import kotlin.math.max

enum class FunModifier {
    VIS, NATIVE, ENTRY, INLINE;
}

@Suppress("UNUSED_PARAMETER")
object MoveParserUtil: GeneratedParserUtilBase() {
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
    fun gtgteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GT_GT_EQ, GT, GT, EQ)

    @JvmStatic
    fun gtgtImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GT_GT, GT, GT)

    @JvmStatic
    fun gteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GT_EQ, GT, EQ)

    @JvmStatic
    fun ltlteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, LT_LT_EQ, LT, LT, EQ)

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
    fun dotdotImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, DOT_DOT, DOT, DOT)

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

    enum class PathParsingMode { VALUE, WILDCARD }

    @JvmStatic
    fun isPathMode(b: PsiBuilder, level: Int, mode: PathParsingMode): Boolean =
        mode == getPathMod(b.flags)

    private fun setPathMod(flags: Int, mode: PathParsingMode): Int {
        val flag = when (mode) {
            PathParsingMode.VALUE -> PATH_MODE_VALUE
            PathParsingMode.WILDCARD -> PATH_MODE_WILDCARD
        }
        return flags and (PATH_MODE_VALUE or PATH_MODE_WILDCARD).inv() or flag
    }

    private fun getPathMod(flags: Int): PathParsingMode = when {
        BitUtil.isSet(flags, PATH_MODE_VALUE) -> PathParsingMode.VALUE
        BitUtil.isSet(flags, PATH_MODE_WILDCARD) -> PathParsingMode.WILDCARD
        // default instead of error()
        else -> PathParsingMode.VALUE
//        else -> error("Path parsing mode not set")
    }

    @JvmStatic
    fun pathMode(b: PsiBuilder, level: Int, mode: PathParsingMode, parser: Parser): Boolean {
        val oldFlags = b.flags
        val newFlags = setPathMod(oldFlags, mode)
        b.flags = newFlags
        check(getPathMod(b.flags) == mode)

        // A hack that reduces the growth rate of `level`. This actually allows a deeper path nesting.
        val prevPathFrame = ErrorState.get(b).currentFrame?.parentFrame?.ancestorOfTypeOrSelf(PATH)
        val nextLevel = if (prevPathFrame != null) {
            max(prevPathFrame.level + 2, level - 9)
        } else {
            level
        }

        val result = parser.parse(b, nextLevel)
        b.flags = oldFlags
        return result
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
        if (nextTokenType in MV_COMMENTS || nextTokenType == WHITE_SPACE) {
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
    fun isResourceAccessEnabled(b: PsiBuilder, level: Int): Boolean = false

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
        val modifiersLeft = FunModifier.entries.toMutableSet()
        if (!native) {
            modifiersLeft.remove(FunModifier.NATIVE)
        }
        var nativeEncountered = false
        var parsed = false

        fun isParsed() = parsed && if (native) nativeEncountered else true

        while (modifiersLeft.isNotEmpty()) {
            when {
                b.tokenType == PUBLIC
                        || isContextualKeyword(b, "friend", FRIEND)
                        || isContextualKeyword(b, "package", PACKAGE) -> {
                    if (FunModifier.VIS !in modifiersLeft) return isParsed()
                    if (!visParser.parse(b, level)) return false
                    modifiersLeft.remove(FunModifier.VIS)
                    parsed = true
                }
                b.tokenType == NATIVE -> {
                    if (FunModifier.NATIVE !in modifiersLeft) return isParsed()
                    modifiersLeft.remove(FunModifier.NATIVE)
                    nativeEncountered = true
                    parsed = true
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

//    @JvmStatic
//    fun parseCastOrIsExpr(
//        b: PsiBuilder,
//        level: Int,
//        exprParser: Parser,
//        allowColon: Boolean
//    ): Boolean {
//        if (!recursion_guard_(b, level, "parseCastOrIsExpr")) return false
//        var result = false
//
//        val isExpr = exprParser.parse(b, level + 1);
//        if (isExpr) {
//
//        }
//    }

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

    @JvmStatic
    fun remapContextualKwOnRollback(b: PsiBuilder, level: Int, p: Parser): Boolean {
        val result = p.parse(b, level)
        if (!result && b.tokenType in CONTEXTUAL_KEYWORDS) {
            b.remapCurrentToken(IDENTIFIER)
        }
        return result
    }

    @Suppress("FunctionName")
    @JvmStatic
    fun vectorIdent(b: PsiBuilder, level: Int): Boolean {
        return nextTokenIs(b, "vector") && consumeToken(b, IDENTIFIER)
    }

    @JvmStatic
    fun assertIdent(b: PsiBuilder, level: Int): Boolean {
        return nextTokenIs(b, "assert") && consumeToken(b, IDENTIFIER)
    }

    @JvmStatic
    fun addressKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "address", ADDRESS)

    @JvmStatic
    fun hasKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "has", HAS)

    @JvmStatic
    fun isKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "is", IS)

    @JvmStatic
    fun entryKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "entry", ENTRY)

    @JvmStatic
    fun inlineKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "inline", INLINE)

    @JvmStatic
    fun schemaKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "schema", SCHEMA_KW)

//    @JvmStatic
//    fun vectorKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "vector", VECTOR_KW)

    @JvmStatic
    fun updateKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "update", UPDATE)

    @JvmStatic
    fun friendKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "friend", FRIEND)

    @JvmStatic
    fun enumKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "enum", ENUM_KW)

    @JvmStatic
    fun matchKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "match", MATCH_KW)

    @JvmStatic
    fun packageKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "package", PACKAGE)

    @JvmStatic
    fun forKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "for", FOR)

    @JvmStatic
    fun readsKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "reads", READS)

    @JvmStatic
    fun writesKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "writes", WRITES)

    @JvmStatic
    fun pureKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "pure", PURE)

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
    fun decreasesKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "decreases", DECREASES)

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
        get() = getUserData(FLAGS) ?: DEFAULT_FLAGS
        set(value) = putUserData(FLAGS, value)

    private fun Int.setFlag(flag: Int, mode: Boolean): Int =
        BitUtil.set(this, flag, mode)

    // flags
    private val TOP_LEVEL: Int = makeBitMask(0)
    private val INCLUDE_STMT_MODE: Int = makeBitMask(1)

    private val PATH_MODE_VALUE: Int = makeBitMask(2)
    private val PATH_MODE_WILDCARD: Int = makeBitMask(3)

//    private val STRUCT_ALLOWED: Int = makeBitMask(4)

    private val DEFAULT_FLAGS: Int = TOP_LEVEL
//    private val DEFAULT_FLAGS: Int = TOP_LEVEL or STRUCT_ALLOWED

    // msl
    private val MSL_LEVEL: Key<Int> = Key("MoveParserUtil.MSL_LEVEL")
    private var PsiBuilder.mslLevel: Int
        get() = getUserData(MSL_LEVEL) ?: 0
        set(value) = putUserData(MSL_LEVEL, value)

    private fun isContextualKeyword(
        b: PsiBuilder,
        keyword: String,
        elementType: IElementType,
        nextElementPredicate: (IElementType?) -> Boolean = { it !in tokenSetOf() }
    ): Boolean {
        return b.tokenType == elementType ||
                b.tokenType == IDENTIFIER && b.tokenText == keyword && nextElementPredicate(b.lookAhead(1))
    }

    private fun contextualKeyword(
        b: PsiBuilder,
        keyword: String,
        elementType: IElementType,
        nextElementPredicate: (IElementType?) -> Boolean = { it !in tokenSetOf() }
    ): Boolean {
        if (isContextualKeyword(b, keyword, elementType, nextElementPredicate)) {
            b.remapCurrentToken(elementType)
            b.advanceLexer()
            return true
        }
        return false
    }

    private tailrec fun Frame.ancestorOfTypeOrSelf(elementType: IElementType): Frame? {
        return if (this.elementType == elementType) {
            this
        } else {
            parentFrame?.ancestorOfTypeOrSelf(elementType)
        }
    }
}
