package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.VoidType

val MoveCallExpr.typeArguments: List<MoveTypeArgument>
    get() =
        this.qualPath.typeArguments

val MoveCallExpr.typeVars: TypeVarsMap
    get() {
        val typeVars = mutableMapOf<String, BaseType?>()
        val referred = this.reference?.resolve() as? MoveTypeParametersOwner ?: return typeVars
        val typeArguments = this.qualPath.typeArguments
        if (referred.typeParameters.size != typeArguments.size) return typeVars

        for ((i, typeArgument) in typeArguments.withIndex()) {
            val name = referred.typeParameters[i].name ?: continue
            val type = typeArgument.type.resolvedType(emptyMap())
            typeVars[name] = type
        }
        return typeVars
    }

abstract class MoveCallExprMixin(node: ASTNode) : MoveQualNameReferenceElementImpl(node),
                                                  MoveCallExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        val signature = this.reference.resolve() as? MoveFunctionSignature ?: return null
        val returnTypeElement = signature.returnType
        if (returnTypeElement == null) {
            return VoidType()
        }
        return returnTypeElement.type?.resolvedType(this.typeVars)
    }
}
