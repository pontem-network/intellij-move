package org.move.lang.core.psi.ref_element

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualPath
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveReference

interface MoveQualTypeReferenceElement : MoveTypeReferenceElement {
    val qualPath: MoveQualPath
}

abstract class MoveQualTypeReferenceElementImpl(node: ASTNode) : MoveReferenceElementImpl(node),
                                                                 MoveQualTypeReferenceElement {

    override fun getReference(): MoveReference = qualPath.reference
}