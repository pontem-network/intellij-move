package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveStructLitExpr
import org.move.lang.core.psi.MoveStructLitField
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveStructFieldReferenceImpl
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.OldMoveReferenceImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MoveStructLitField.structLit: MoveStructLitExpr
    get() = ancestorStrict()!!

val MoveStructLitField.isShorthand: Boolean
    get() = structLitFieldAssignment == null

fun MoveStructLitField.inferAssignedExprTy(ctx: InferenceContext): Ty {
    val assignment = this.structLitFieldAssignment
    return if (assignment == null) {
        // find type of binding
        val resolved = this.reference.resolve() as? MoveBindingPat ?: return TyUnknown
        resolved.inferBindingPatTy()
    } else {
        // find type of expression
        assignment.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown
    }
}

abstract class MoveStructLitFieldMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                        MoveStructLitField {
    override fun getReference(): MoveReference {
        if (this.isShorthand) {
            return OldMoveReferenceImpl(this, Namespace.NAME)
        }
        return MoveStructFieldReferenceImpl(this)
    }
}
