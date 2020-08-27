package org.move.ide

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.MoveElementTypes.*
import org.move.lang.core.MV_COMMENTS

class MoveBraceMatcher : PairedBraceMatcher {
    override fun getPairs() = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, next: IElementType?): Boolean =
        next in InsertPairBraceBefore

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    companion object {
        private val PAIRS: Array<BracePair> = arrayOf(
            BracePair(L_BRACE, R_BRACE, true /* structural */),
            BracePair(L_PAREN, R_PAREN, false),
            BracePair(L_BRACK, R_BRACK, false),
            BracePair(LT, GT, false)
        )

        private val InsertPairBraceBefore = TokenSet.orSet(
            MV_COMMENTS,
            TokenSet.create(
                TokenType.WHITE_SPACE,
                SEMICOLON,
                COMMA,
                R_PAREN,
                R_BRACK,
                R_BRACE, L_BRACE
            )
        )
    }
}
