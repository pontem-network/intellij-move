package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveParensExpr
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap

abstract class MoveParensExprMixin(node: ASTNode): MoveElementImpl(node), MoveParensExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.expr?.resolvedType(typeVars)
    }

}
