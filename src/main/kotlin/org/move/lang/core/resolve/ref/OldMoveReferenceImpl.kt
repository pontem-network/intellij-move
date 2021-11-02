package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.resolve.resolveItem

class OldMoveReferenceImpl(
    element: MoveReferenceElement,
    private val namespace: Namespace,
) : MoveReferenceBase<MoveReferenceElement>(element) {

    override fun resolve(): MoveNamedElement? {
        return resolveItem(element, namespace)
    }
}
