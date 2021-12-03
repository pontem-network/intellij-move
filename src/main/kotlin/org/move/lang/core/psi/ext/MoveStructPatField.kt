package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MovePat
import org.move.lang.core.psi.MoveStructPat
import org.move.lang.core.psi.MoveStructPatField
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveStructFieldReferenceImpl

val MoveStructPatField.structPat: MoveStructPat
    get() = ancestorStrict()!!

val MoveStructPatField.pat: MovePat? get() {
    return this.bindingPat ?: this.structPatFieldBinding?.pat
}

abstract class MoveStructPatFieldMixin(node: ASTNode) : MoveElementImpl(node),
                                                        MoveStructPatField {
    override val referenceNameElement: PsiElement
        get() {
            val bindingPat = this.bindingPat
            if (bindingPat != null) {
                return bindingPat.identifier
            } else {
                return this.identifier
            }
        }

    override fun getReference(): MoveReference {
        return MoveStructFieldReferenceImpl(this)
    }
}
