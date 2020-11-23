package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveStructFieldReferenceElement
import org.move.lang.core.resolve.resolveItem

class MoveStructFieldReferenceImpl(
    element: MoveStructFieldReferenceElement,
) : MoveReferenceBase<MoveStructFieldReferenceElement>(element) {

    override fun resolve(): MoveNamedElement? = resolveItem(element, Namespace.STRUCT_FIELD)
}