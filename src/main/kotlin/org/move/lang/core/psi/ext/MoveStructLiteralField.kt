package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveStructFieldReferenceImpl
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.OldMoveReferenceImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MoveStructLiteralField.structLiteral: MoveStructLiteralExpr
    get() = ancestorStrict()!!

val MoveStructLiteralField.isShorthand: Boolean
    get() = structLiteralFieldAssignment == null

fun MoveStructLiteralField.inferAssignedExprTy(ctx: InferenceContext): Ty {
    val assignment = this.structLiteralFieldAssignment
    return if (assignment == null) {
        // find type of binding
        val resolved = this.reference.resolve() as? MoveBindingPat ?: return TyUnknown
        resolved.inferBindingPatTy()
    } else {
        // find type of expression
        assignment.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown
    }
}

abstract class MoveStructLiteralFieldMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                            MoveStructLiteralField {
    override fun getReference(): MoveReference {
        if (this.isShorthand) {
            return OldMoveReferenceImpl(this, Namespace.NAME)
        }
        return MoveStructFieldReferenceImpl(this)
    }
}
