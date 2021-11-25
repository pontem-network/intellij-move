package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.IntegerType
import org.move.lang.core.types.PrimitiveType
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyBool
import org.move.lang.core.types.ty.TyInteger
import org.move.lang.core.types.ty.TyUnknown

fun binaryExprType(exprList: List<MoveExpr>, typeVars: TypeVarsMap): Ty {
    for ((i, expr) in exprList.withIndex()) {
        val exprType = expr.resolvedType(typeVars)
        if (exprType is TyInteger && exprType.kind == TyInteger.DEFAULT_KIND) {
            if (i == exprList.lastIndex) return exprType
            continue
        }
        return exprType
    }
    return TyUnknown
}


abstract class MovePlusExprMixin(node: ASTNode): MoveElementImpl(node), MovePlusExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveMinusExprMixin(node: ASTNode): MoveElementImpl(node), MoveMinusExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveMulExprMixin(node: ASTNode): MoveElementImpl(node), MoveMulExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveDivExprMixin(node: ASTNode): MoveElementImpl(node), MoveDivExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveModExprMixin(node: ASTNode): MoveElementImpl(node), MoveModExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return binaryExprType(this.exprList, typeVars)
    }
}

abstract class MoveBooleanExprMixin(node: ASTNode): MoveElementImpl(node), MoveExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return TyBool
    }
}
