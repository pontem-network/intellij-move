package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveDerefExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyUnknown

abstract class MoveDerefExprMixin(node: ASTNode) : MoveElementImpl(node),
                                                   MoveDerefExpr {
    override fun resolvedType(): Ty {
        val innerRefType = this.expr?.resolvedType() as? TyReference ?: return TyUnknown
        return innerRefType.referenced
    }
}
