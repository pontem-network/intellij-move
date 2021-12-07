package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.MoveStructLiteralFieldsBlock
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.ty.Ty

val MoveStructLiteralExpr.providedFields: List<MoveStructLiteralField>
    get() =
        structLiteralFieldsBlock.structLiteralFieldList

val MoveStructLiteralExpr.providedFieldNames: List<String>
    get() =
        providedFields.map { it.referenceName }

val MoveStructLiteralFieldsBlock.litExpr: MoveStructLiteralExpr
    get() = this.parent as MoveStructLiteralExpr
