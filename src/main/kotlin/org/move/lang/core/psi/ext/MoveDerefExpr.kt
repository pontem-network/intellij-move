package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveDerefExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.RefType
import org.move.lang.core.types.TypeVarsMap

abstract class MoveDerefExprMixin(node: ASTNode) : MoveElementImpl(node),
                                                   MoveDerefExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        val innerExpr = this.expr ?: return null
        val innerRefType = innerExpr.resolvedType(emptyMap()) as? RefType ?: return null
        return innerRefType.referredType
    }
}
