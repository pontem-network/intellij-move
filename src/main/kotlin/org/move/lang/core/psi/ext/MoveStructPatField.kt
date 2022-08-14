package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvPat
import org.move.lang.core.psi.MvStructPat
import org.move.lang.core.psi.MvStructPatField
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvStructFieldReferenceImpl
import org.move.lang.core.resolve.ref.MvStructPatShorthandFieldReferenceImpl

val MvStructPatField.structPat: MvStructPat
    get() = ancestorStrict()!!

val MvStructPatField.pat: MvPat? get() {
    return this.bindingPat ?: this.structPatFieldBinding?.pat
}

val MvStructPatField.isShorthand: Boolean get() = this.structPatFieldBinding == null

abstract class MvStructPatFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvStructPatField {
    override val referenceNameElement: PsiElement
        get() {
            val bindingPat = this.bindingPat
            if (bindingPat != null) {
                return bindingPat.identifier
            } else {
                return this.identifier
            }
        }

    override fun getReference(): MvReference {
        return if (this.isShorthand) {
            MvStructPatShorthandFieldReferenceImpl(this)
        } else {
            MvStructFieldReferenceImpl(this)
        }
    }
}
