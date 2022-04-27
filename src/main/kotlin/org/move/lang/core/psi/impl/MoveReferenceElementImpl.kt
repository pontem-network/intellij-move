package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.resolve.MvReferenceElement
import org.move.lang.core.resolve.ref.MvReference

abstract class MvReferenceElementImpl(node: ASTNode) : MvElementImpl(node),
                                                       MvReferenceElement {
    abstract override fun getReference(): MvReference
}
