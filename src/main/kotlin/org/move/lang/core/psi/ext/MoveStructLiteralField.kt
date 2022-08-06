package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvStructFieldReferenceImpl
import org.move.lang.core.resolve.ref.MvStructLitShorthandFieldReferenceImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferStructLitExpr
import org.move.lang.core.types.infer.inferenceCtx
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown

val MvStructLitField.structLitExpr: MvStructLitExpr
    get() = ancestorStrict()!!

val MvStructLitField.isShorthand: Boolean
    get() = !hasChild(MvElementTypes.COLON)

fun MvStructLitField.ty(): Ty {
    val msl = this.isMsl()
    val ctx = this.containingFunction?.inferenceCtx(msl) ?: return TyUnknown
    val structLitTy = inferStructLitExpr(this.structLitExpr, ctx) as? TyStruct ?: return TyUnknown
    return structLitTy.fieldTy(this.referenceName, msl)
}

abstract class MvStructLitFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvStructLitField {
    override fun getReference(): MvReference {
        if (!this.isShorthand) return MvStructFieldReferenceImpl(this)
        return MvStructLitShorthandFieldReferenceImpl(this)
    }
}
