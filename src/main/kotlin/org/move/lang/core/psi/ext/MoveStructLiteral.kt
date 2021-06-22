package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.StructType
import org.move.lang.core.types.TypeVarsMap
import java.sql.Struct

val MoveStructLiteralExpr.providedFields: List<MoveStructLiteralField>
    get() =
        structLiteralFieldsBlock.structLiteralFieldList

val MoveStructLiteralExpr.providedFieldNames: List<String>
    get() =
        providedFields.mapNotNull { it.referenceName }

abstract class MoveStructLiteralExprMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                           MoveStructLiteralExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        val signature = this.referredStructSignature ?: return null
        val typeArgs = this.qualPath.typeArguments
        if (typeArgs.size < signature.typeParameters.size) {
            val typeParamTypes = signature.typeParameters.map { it.typeParamType }
            return StructType(signature, typeParamTypes)
        } else {
            val typeArgumentTypes =
                typeArgs.map { it.type.resolvedType(emptyMap()) }
            return StructType(signature, typeArgumentTypes)
        }
    }
}
