package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveStructLitExpr
import org.move.lang.core.psi.MoveStructLitField
import org.move.lang.core.psi.MoveStructLitFieldsBlock

val MoveStructLitExpr.providedFields: List<MoveStructLitField>
    get() =
        structLitFieldsBlock.structLitFieldList

val MoveStructLitExpr.providedFieldNames: List<String>
    get() =
        providedFields.map { it.referenceName }

val MoveStructLitFieldsBlock.litExpr: MoveStructLitExpr
    get() = this.parent as MoveStructLitExpr
