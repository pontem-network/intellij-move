package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvBreakExpr
import org.move.lang.core.psi.MvElementImpl

abstract class MvBreakExprMixin(node: ASTNode): MvElementImpl(node), MvBreakExpr {

    override val operator: PsiElement get() = `break`
}