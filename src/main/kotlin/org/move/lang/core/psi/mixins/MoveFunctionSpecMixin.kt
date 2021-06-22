package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveFunctionSpecDef
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl
import org.move.lang.core.resolve.ref.Namespace

abstract class MoveFunctionSpecMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                      MoveFunctionSpecDef {

    override fun getReference(): MoveReference = MoveReferenceImpl(this, Namespace.NAME)
}
