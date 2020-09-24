package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructSpec
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.psi.ref_element.MoveTypeReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl
import org.move.lang.core.resolve.ref.Namespace

abstract class MoveStructSpecMixin(node: ASTNode) : MoveTypeReferenceElementImpl(node),
                                                    MoveStructSpec {
    override fun getReference(): MoveReference =
        MoveReferenceImpl(this, Namespace.TYPE)

}