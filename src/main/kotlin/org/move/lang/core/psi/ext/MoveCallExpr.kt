package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnit
import org.move.lang.core.types.ty.TyUnknown

val MoveCallExpr.typeArguments: List<MoveTypeArgument>
    get() {
        return this.path.typeArguments
    }

val MoveCallExpr.arguments: List<MoveExpr>
    get() {
        return this.callArguments?.exprList.orEmpty()
    }

val MoveCallExpr.typeVars: TypeVarsMap
    get() {
        val typeVars = mutableMapOf<String, Ty>()
        val referred = this.path.reference?.resolve() as? MoveTypeParametersOwner ?: return typeVars
        val typeArguments = this.path.typeArguments
        if (referred.typeParameters.size != typeArguments.size) return typeVars

        for ((i, typeArgument) in typeArguments.withIndex()) {
            val name = referred.typeParameters[i].name ?: continue
            val type = typeArgument.type.resolvedType(emptyMap())
            typeVars[name] = type
        }
        return typeVars
    }

abstract class MoveCallExprMixin(node: ASTNode) : MoveElementImpl(node), MoveCallExpr {

    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        val signature = this.path.reference?.resolve() as? MoveFunctionSignature ?: return TyUnknown
        val returnTypeElement = signature.returnType
        if (returnTypeElement == null) {
            return TyUnit
        }

        return returnTypeElement.type?.resolvedType(this.typeVars) ?: TyUnknown
    }
}
