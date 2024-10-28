package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvContinueExpr
import org.move.lang.core.psi.MvElementImpl

abstract class MvContinueExprMixin(node: ASTNode): MvElementImpl(node), MvContinueExpr {

    override val operator: PsiElement get() = `continue`
}