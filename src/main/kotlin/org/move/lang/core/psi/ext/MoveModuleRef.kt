package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveModuleRef
import org.move.lang.core.psi.MoveQualNameReferenceElementImpl
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveModuleReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference

//val MoveFullyQualifiedModuleRef.hasAddress: Boolean get() = addressRef?.address() != null


abstract class MoveModuleRefMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                   MoveModuleRef {
    override fun getReference(): MoveReference =
        MoveModuleReferenceImpl(this)
}