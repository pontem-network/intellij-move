package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvStructLitExpr
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvStructFieldReferenceImpl
import org.move.lang.core.resolve.ref.MvStructLitShorthandFieldReferenceImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvStructLitField.structLit: MvStructLitExpr
    get() = ancestorStrict()!!

val MvStructLitField.isShorthand: Boolean
    get() = !hasChild(MvElementTypes.COLON)

fun MvStructLitField.inferInitExprTy(ctx: InferenceContext): Ty {
    val initExpr = this.expr
    return if (initExpr == null) {
        // find type of binding
        val resolved =
            this.reference.multiResolve().filterIsInstance<MvBindingPat>().firstOrNull() ?: return TyUnknown
        resolved.ty(ctx.msl)
    } else {
        // find type of expression
        initExpr.inferExprTy(ctx)
    }
}

abstract class MvStructLitFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvStructLitField {
    override fun getReference(): MvReference {
        if (!this.isShorthand) return MvStructFieldReferenceImpl(this)
        return MvStructLitShorthandFieldReferenceImpl(this)
    }
}
