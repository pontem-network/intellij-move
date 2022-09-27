package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvStructLitExpr
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvStructFieldReferenceImpl
import org.move.lang.core.resolve.ref.MvStructLitShorthandFieldReferenceImpl

val MvStructLitField.structLitExpr: MvStructLitExpr
    get() = ancestorStrict()!!

val MvStructLitField.isShorthand: Boolean
    get() = !hasChild(MvElementTypes.COLON)

abstract class MvStructLitFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvStructLitField {
    override fun getReference(): MvReference {
        if (!this.isShorthand) return MvStructFieldReferenceImpl(this)
        return MvStructLitShorthandFieldReferenceImpl(this)
    }
}
