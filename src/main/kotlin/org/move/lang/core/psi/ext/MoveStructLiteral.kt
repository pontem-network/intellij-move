package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown

val MoveStructLiteralExpr.providedFields: List<MoveStructLiteralField>
    get() =
        structLiteralFieldsBlock.structLiteralFieldList

val MoveStructLiteralExpr.providedFieldNames: List<String>
    get() =
        providedFields.map { it.referenceName }

abstract class MoveStructLiteralExprMixin(node: ASTNode) : MoveElementImpl(node), MoveStructLiteralExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        val signature = this.path.maybeStructSignature ?: return TyUnknown
        val typeArgs = this.path.typeArguments
        if (typeArgs.size < signature.typeParameters.size) {
            val typeParamTypes = signature.typeParameters.map { it.typeParamType }
            return TyStruct(signature, typeParamTypes)
        } else {
            val typeArgumentTypes =
                typeArgs.map { it.type.resolvedType(emptyMap()) }
            return TyStruct(signature, typeArgumentTypes)
        }
    }
}
