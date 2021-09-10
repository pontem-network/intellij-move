package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.IntegerType
import org.move.lang.core.types.PrimitiveType
import org.move.lang.core.types.TypeVarsMap

fun binaryExprType(exprList: List<MoveExpr>, typeVars: TypeVarsMap): BaseType? {
    for ((i, expr) in exprList.withIndex()) {
        val exprType = expr.resolvedType(typeVars)
        if (exprType is IntegerType && exprType.precision == null) {
            if (i == exprList.lastIndex) return exprType
            continue
        }
        return exprType
    }
    return null
}


abstract class MovePlusExprMixin(node: ASTNode): MoveElementImpl(node), MovePlusExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveMinusExprMixin(node: ASTNode): MoveElementImpl(node), MoveMinusExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveMulExprMixin(node: ASTNode): MoveElementImpl(node), MoveMulExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveDivExprMixin(node: ASTNode): MoveElementImpl(node), MoveDivExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveModExprMixin(node: ASTNode): MoveElementImpl(node), MoveModExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveBooleanExprMixin(node: ASTNode): MoveElementImpl(node), MoveExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return PrimitiveType("bool")
    }
}
