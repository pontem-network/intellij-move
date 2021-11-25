package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveParensExpr
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

abstract class MoveParensExprMixin(node: ASTNode) : MoveElementImpl(node), MoveParensExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return this.expr?.resolvedType(typeVars) ?: TyUnknown
    }

}
