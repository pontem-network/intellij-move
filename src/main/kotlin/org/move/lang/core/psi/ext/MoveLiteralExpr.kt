package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.HEX_INTEGER_LITERAL
import org.move.lang.core.psi.MoveLiteralExpr

val MoveLiteralExpr.hexIntegerLiteral: PsiElement?
    get() =
        this.findFirstChildByType(HEX_INTEGER_LITERAL)
