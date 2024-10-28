package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.BINARY_OP
import org.move.lang.core.psi.MvBinaryExpr
import org.move.lang.core.psi.MvElementImpl

val MvBinaryExpr.operator: PsiElement get() = binaryOp.operator

abstract class MvBinaryExprMixin(node: ASTNode): MvElementImpl(node),
                                                 MvBinaryExpr {
    override fun toString(): String {
        val op = node.findChildByType(BINARY_OP)?.text ?: ""
        return "${javaClass.simpleName}(${node.elementType}[$op])"
    }
}
