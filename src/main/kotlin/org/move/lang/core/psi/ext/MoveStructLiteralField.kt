package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvStructLitExpr
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvStructFieldReferenceImpl
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.OldMvReferenceImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvStructLitField.structLit: MvStructLitExpr
    get() = ancestorStrict()!!

val MvStructLitField.isShorthand: Boolean
    get() = fieldInit == null

fun MvStructLitField.inferInitExprTy(ctx: InferenceContext): Ty {
    val init = this.fieldInit
    return if (init == null) {
        // find type of binding
        val resolved = this.reference.resolve() as? MvBindingPat ?: return TyUnknown
        resolved.inferBindingPatTy()
    } else {
        // find type of expression
        init.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown
    }
}

abstract class MvStructLitFieldMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                        MvStructLitField {
    override fun getReference(): MvReference {
        if (this.isShorthand) {
            return OldMvReferenceImpl(this, Namespace.NAME)
        }
        return MvStructFieldReferenceImpl(this)
    }
}
