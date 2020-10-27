package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualTypeReferenceElementImpl
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralField

val MoveStructLiteralExpr.providedFields: List<MoveStructLiteralField>
    get() =
        structLiteralFieldsBlock.structLiteralFieldList

val MoveStructLiteralExpr.providedFieldNames: List<String>
    get() =
        providedFields.map { it.referenceName }

abstract class MoveStructLiteralExprMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                           MoveStructLiteralExpr