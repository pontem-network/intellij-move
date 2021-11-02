package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.OldMoveReferenceImpl
import org.move.lang.core.resolve.ref.MoveStructFieldReferenceImpl
import org.move.lang.core.resolve.ref.Namespace

val MoveStructLiteralField.structLiteral: MoveStructLiteralExpr
    get() = ancestorStrict()!!

val MoveStructLiteralField.isShorthand: Boolean
    get() = structLiteralFieldAssignment == null

abstract class MoveStructLiteralFieldMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                            MoveStructLiteralField {
    override fun getReference(): MoveReference {
        if (this.isShorthand) {
            return OldMoveReferenceImpl(this, Namespace.NAME)
        }
        return MoveStructFieldReferenceImpl(this)
    }
}
