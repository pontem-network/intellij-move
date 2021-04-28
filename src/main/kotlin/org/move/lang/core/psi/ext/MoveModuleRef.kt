package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveModuleRef
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveModuleReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference


abstract class MoveModuleRefMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                   MoveModuleRef {
    override fun getReference(): MoveReference {
        return MoveModuleReferenceImpl(this)
    }

    override val isUnresolved: Boolean
        get() = super<MoveReferenceElementImpl>.isUnresolved
}
