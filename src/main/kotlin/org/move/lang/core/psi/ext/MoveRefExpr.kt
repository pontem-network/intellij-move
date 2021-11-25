package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.types.ty.HasType
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

abstract class MoveRefExprMixin(node: ASTNode) : MoveElementImpl(node), MoveRefExpr {

    override fun resolvedType(): Ty {
        val refTyped =
            this.path.reference?.resolve() as? HasType ?: return TyUnknown
        return refTyped.resolvedType()
    }
}
