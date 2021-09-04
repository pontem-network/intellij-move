package org.move.lang.core

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderUtil
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.MoveElementTypes.*

@Suppress("UNUSED_PARAMETER")
object MoveParserUtil : GeneratedParserUtilBase() {
//    @JvmStatic
//    fun gtgteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GTGTEQ, GT, GT, EQ)

    @JvmStatic
    fun gtgtImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GT_GT, GT, GT)

    @JvmStatic
    fun gteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GT_EQ, GT, EQ)

//    @JvmStatic
//    fun ltlteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, LTLTEQ, LT, LT, EQ)

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
            b.advanceLexer();
            return true
        }
        if (b.tokenType != ADDRESS_IDENT) return false

        b.remapCurrentToken(HEX_INTEGER_LITERAL)
        b.advanceLexer()
        return true
    }

    @JvmStatic
    fun patternIdent(b: PsiBuilder, level: Int): Boolean {
        if (b.tokenType == FUNCTION_PATTERN_IDENT) { b.advanceLexer(); return true; }
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
            b.advanceLexer();
            return true;
        };
        if (b.tokenType == IDENTIFIER && b.tokenText == "internal") {
            b.remapCurrentToken(INTERNAL);
            b.advanceLexer();
            return true;
        }
        return false;
    }

    @JvmStatic
    fun invariantModifierKeyword(b: PsiBuilder, level: Int): Boolean {
        if (b.tokenType in tokenSetOf(PACK, UNPACK, UPDATE)) {
            b.advanceLexer();
            return true;
        };
        if (b.tokenType != IDENTIFIER) return false;

        val tokenType = when (b.tokenText) {
            "pack" -> PACK
            "unpack" -> UNPACK
            "update" -> UPDATE
            else -> {
                return false
            }
        }
        b.remapCurrentToken(tokenType);
        b.advanceLexer();
        return true;
    }

    @JvmStatic
    fun addressKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "address", ADDRESS)

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
    fun invariantKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "invariant", INVARIANT)

    @JvmStatic
    fun axiomKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "axiom", AXIOM)

    @JvmStatic
    fun abortsIfKeyword(b: PsiBuilder, level: Int): Boolean = contextualKeyword(b, "aborts_if", ABORTS_IF)

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
            b.advanceLexer();
            return true
        }
        return false
    }

}
