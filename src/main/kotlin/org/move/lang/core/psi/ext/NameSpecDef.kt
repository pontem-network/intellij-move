package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveNameSpecDef
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl
import org.move.lang.core.resolve.ref.Namespace

abstract class MoveNameSpecDefMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                     MoveNameSpecDef {
    override fun getReference(): MoveReference {
        return MoveReferenceImpl(this, Namespace.NAME)
    }
}
