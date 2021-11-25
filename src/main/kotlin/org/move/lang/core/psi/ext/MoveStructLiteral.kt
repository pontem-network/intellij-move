package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.StructType
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown
import java.sql.Struct

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
