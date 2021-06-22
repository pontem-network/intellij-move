package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveCastExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap

abstract class MoveCastExprMixin(node: ASTNode): MoveElementImpl(node), MoveCastExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.type.resolvedType(typeVars)
    }
}
