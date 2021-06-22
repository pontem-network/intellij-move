package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap


abstract class MovePlusExprMixin(node: ASTNode): MoveElementImpl(node), MovePlusExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.exprList.firstOrNull()?.resolvedType(typeVars)
    }
}

abstract class MoveMinusExprMixin(node: ASTNode): MoveElementImpl(node), MoveMinusExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.exprList.firstOrNull()?.resolvedType(typeVars)
    }
}

abstract class MoveMulExprMixin(node: ASTNode): MoveElementImpl(node), MoveMulExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.exprList.firstOrNull()?.resolvedType(typeVars)
    }
}

abstract class MoveDivExprMixin(node: ASTNode): MoveElementImpl(node), MoveDivExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.exprList.firstOrNull()?.resolvedType(typeVars)
    }
}

abstract class MoveModExprMixin(node: ASTNode): MoveElementImpl(node), MoveModExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.exprList.firstOrNull()?.resolvedType(typeVars)
    }
}
