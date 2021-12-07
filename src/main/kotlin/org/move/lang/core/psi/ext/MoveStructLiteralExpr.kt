package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStructLitExpr
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.psi.MvStructLitFieldsBlock

val MvStructLitExpr.providedFields: List<MvStructLitField>
    get() =
        structLitFieldsBlock.structLitFieldList

val MvStructLitExpr.providedFieldNames: List<String>
    get() =
        providedFields.map { it.referenceName }

val MvStructLitFieldsBlock.litExpr: MvStructLitExpr
    get() = this.parent as MvStructLitExpr
