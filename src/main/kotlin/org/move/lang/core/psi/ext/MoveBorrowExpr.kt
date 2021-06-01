package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveBorrowExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.RefType

abstract class MoveBorrowExprMixin(node: ASTNode) : MoveElementImpl(node),
                                                    MoveBorrowExpr {
    override fun resolvedType(): BaseType? {
        val innerExpr = this.expr ?: return null
        val innerExprType = innerExpr.resolvedType() ?: return null
        return RefType(innerExprType, "mut" in this.text)
    }
}
