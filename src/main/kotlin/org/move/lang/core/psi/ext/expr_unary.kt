package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveBangExpr
import org.move.lang.core.psi.MoveCopyExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveMoveExpr
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyBool
import org.move.lang.core.types.ty.TyUnknown

abstract class MoveBangExprMixin(node: ASTNode) : MoveElementImpl(node), MoveBangExpr {
    override fun resolvedType(): Ty {
        return TyBool
    }
}

abstract class MoveCopyExprMixin(node: ASTNode) : MoveElementImpl(node), MoveCopyExpr {
    override fun resolvedType(): Ty {
        return this.expr?.resolvedType() ?: TyUnknown
//        return this.expr?.resolvedType(typeVars)
    }
}

abstract class MoveMoveExprMixin(node: ASTNode) : MoveElementImpl(node), MoveMoveExpr {
    override fun resolvedType(): Ty {
        return this.expr?.resolvedType() ?: TyUnknown
    }
}
