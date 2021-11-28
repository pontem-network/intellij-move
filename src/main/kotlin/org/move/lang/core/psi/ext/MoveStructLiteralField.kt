package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveFunctionParameter
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.types.ty.HasType
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MoveStructLiteralField.structLiteral: MoveStructLiteralExpr
    get() = ancestorStrict()!!

val MoveStructLiteralField.isShorthand: Boolean
    get() = structLiteralFieldAssignment == null

val MoveStructLiteralField.assignedExprTy: Ty
    get() {
        val assignment = this.structLiteralFieldAssignment
        return if (assignment == null) {
            // find type of binding
            val resolved = this.reference.resolve() as? HasType ?: return TyUnknown
            resolved.resolvedType()
        } else {
            // find type of expression
            assignment.expr?.resolvedType() ?: TyUnknown
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
