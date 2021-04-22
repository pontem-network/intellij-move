package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveFullyQualifiedModuleRef
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveQualModuleReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference

abstract class MoveFullyQualifiedModuleRefMixin(
    node: ASTNode,
) :
    MoveReferenceElementImpl(node),
    MoveFullyQualifiedModuleRef {

    override fun getReference(): MoveReference {
        return MoveQualModuleReferenceImpl(this)
    }
}
