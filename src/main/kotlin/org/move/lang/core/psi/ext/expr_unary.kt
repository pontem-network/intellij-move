package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveBangExpr
import org.move.lang.core.psi.MoveCopyExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveMoveExpr
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.PrimitiveType
import org.move.lang.core.types.TypeVarsMap

abstract class MoveBangExprMixin(node: ASTNode): MoveElementImpl(node), MoveBangExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return PrimitiveType("bool")
    }
}

abstract class MoveCopyExprMixin(node: ASTNode): MoveElementImpl(node), MoveCopyExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.expr?.resolvedType(typeVars)
    }
}

abstract class MoveMoveExprMixin(node: ASTNode): MoveElementImpl(node), MoveMoveExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.expr?.resolvedType(typeVars)
    }
}
