package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType

val MoveStructLiteralExpr.providedFields: List<MoveStructLiteralField>
    get() =
        structLiteralFieldsBlock.structLiteralFieldList

val MoveStructLiteralExpr.providedFieldNames: List<String>
    get() =
        providedFields.mapNotNull { it.referenceName }

abstract class MoveStructLiteralExprMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                           MoveStructLiteralExpr {
    override fun resolvedType(): BaseType? {
        return this.referredStructDef?.structType
    }
}
