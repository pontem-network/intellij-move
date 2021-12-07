package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.HEX_INTEGER_LITERAL
import org.move.lang.core.psi.MvLitExpr

val MvLitExpr.hexIntegerLiteral: PsiElement?
    get() =
        this.findFirstChildByType(HEX_INTEGER_LITERAL)
