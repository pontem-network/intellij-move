package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveBorrowExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Mutability
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyUnknown

val MoveBorrowExpr.isMut: Boolean
    get() {
        return childrenByType(MoveElementTypes.MUT).toList().isNotEmpty()
    }

abstract class MoveBorrowExprMixin(node: ASTNode) : MoveElementImpl(node),
                                                    MoveBorrowExpr {
    override fun resolvedType(): Ty {
        val innerExpr = this.expr ?: return TyUnknown
        val innerExprType = innerExpr.resolvedType()
        return TyReference(
            innerExprType,
            Mutability.valueOf("mut" in this.text)
        )
    }
}
