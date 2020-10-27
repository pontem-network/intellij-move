package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructPat
import org.move.lang.core.psi.MoveStructPatField
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveStructFieldReferenceImpl

val MoveStructPatField.structPat: MoveStructPat
    get() = ancestorStrict()!!

abstract class MoveStructPatFieldMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                        MoveStructPatField {
    override fun getReference(): MoveReference {
        return MoveStructFieldReferenceImpl(this)
    }
}