package org.move.lang.core.psi.ref_element

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.impl.MoveReferenceElementImpl

interface MoveTypeReferenceElement : MoveReferenceElement {
}

abstract class MoveTypeReferenceElementImpl(node: ASTNode) : MoveReferenceElementImpl(node),
                                                             MoveTypeReferenceElement