package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualTypeReferenceElementImpl
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.referredStructSignature
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.StructType
import org.move.lang.core.types.TypeVarsMap

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
        val typeArgumentTypes =
            this.qualPath.typeArguments
                .map { it.type.resolvedType(emptyMap()) }
        return StructType(signature, typeArgumentTypes)
    }
}
