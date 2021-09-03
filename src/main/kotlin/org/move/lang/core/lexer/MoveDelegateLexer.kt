package org.move.lang.core.lexer

import com.intellij.lexer.*
import com.intellij.psi.tree.IElementType
import org.move.lang.MoveElementTypes.*
import org.move.lang._MoveLexer

//val SPEC_KEYWORDS = mapOf(
////    "global" to GLOBAL,
////    "local" to LOCAL,
//    "assert" to ASSERT,
//    "assume" to ASSUME,
//    "invariant" to INVARIANT,
//    "requires" to REQUIRES,
//    "modifies" to MODIFIES,
////    "forall" to FORALL,
////    "exists" to EXISTS,
////    "define" to DEFINE,
//    "pragma" to PRAGMA,
//    "apply" to APPLY,
//    "aborts_if" to ABORTS_IF,
//)


@Suppress("UnstableApiUsage")
class MoveDelegateLexer : RestartableLexer,
                          DelegateLexer(FlexAdapter(_MoveLexer(null))) {
    //    private var isSpecBlock: Boolean = false
    private var depth: Int = 0
//    private var specBracesDepth: Int = 0

    override fun getStartState(): Int {
        return _MoveLexer.YYINITIAL
    }

    override fun getState(): Int {
        val innerState = super.getState()
        return innerState + depth * 10
    }

    override fun start(
        buffer: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int,
        tokenIterator: TokenIterator?,
    ) {
        val innerState = initialState % 10
        depth = initialState / 10

        super.start(buffer, startOffset, endOffset, innerState)
    }

    override fun isRestartableState(state: Int): Boolean {
        return state % 10 == _MoveLexer.YYINITIAL
    }

//    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
//        val innerState = initialState % 10
//        depth = initialState / 10
//        super.start(buffer, startOffset, endOffset, innerState)
//    }

//    override fun getState(): Int {
//        val innerState = super.getState()
//        return innerState + depth * 10
//    }

    //    private fun changeSpecBlockState(tokenType: IElementType?) {
//        if (!isSpecBlock && tokenType == SPEC) {
//            specBracesDepth = 0
//            isSpecBlock = true
//        }
//
//        if (isSpecBlock && tokenType == L_BRACE) {
//            specBracesDepth += 1
//        }
//        if (isSpecBlock && tokenType == R_BRACE) {
//            specBracesDepth -= 1
//            if (specBracesDepth == 0) {
//                isSpecBlock = false
//            }
//        }
//    }
//
//    override fun getTokenType(): IElementType? {
//        val tokenType = super.getTokenType()
//
//        if (tokenType == ADDRESS && depth > 0) {
//            return IDENTIFIER
//        }
//
//        if (tokenType == L_BRACE) depth += 1
//        if (tokenType == R_BRACE) depth -= 1
////
////        if (tokenType == ADDRESS && depth > 0) {
////            return IDENTIFIER
////        }
////
////        changeSpecBlockState(tokenType)
////
////        if (isSpecBlock) {
////            if (tokenType == IDENTIFIER && super.getTokenText() in SPEC_KEYWORDS.keys) {
////                return SPEC_KEYWORDS.get(super.getTokenText())
////            }
////        }
//        return tokenType
//    }
}

fun createMoveLexer(): Lexer = MoveDelegateLexer()
