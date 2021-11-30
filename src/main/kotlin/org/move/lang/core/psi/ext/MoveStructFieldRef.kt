package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveStructFieldRef
import org.move.lang.core.psi.MoveStructFieldReferenceElement
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceBase
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveItem

class MoveDotStructFieldAccessReferenceImpl(
    element: MoveStructFieldReferenceElement
) : MoveReferenceBase<MoveStructFieldReferenceElement>(element) {

    override fun resolve(): MoveNamedElement? = resolveItem(element, Namespace.DOT_ACCESSED_FIELD)
}

abstract class MoveStructFieldRefMixin(node: ASTNode) : MoveElementImpl(node),
                                                        MoveStructFieldRef {
    override fun getReference(): MoveReference {
        return MoveDotStructFieldAccessReferenceImpl(this)
    }
}
