package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveSchemaReferenceElement
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl
import org.move.lang.core.resolve.ref.MoveReferenceKind

abstract class MoveSchemaReferenceElementImpl(node: ASTNode) : MoveReferenceElementImpl(node),
                                                               MoveSchemaReferenceElement {
    override fun getReference(): MoveReference =
        MoveReferenceImpl(this, MoveReferenceKind.SCHEMA)
}