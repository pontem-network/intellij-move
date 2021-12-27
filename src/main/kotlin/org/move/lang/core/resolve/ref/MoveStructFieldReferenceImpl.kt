package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructFieldReferenceElement
import org.move.lang.core.resolve.resolveItem

class MvStructFieldReferenceImpl(
    element: MvStructFieldReferenceElement,
) : MvReferenceCached<MvStructFieldReferenceElement>(element) {

    override fun resolveInner(): MvNamedElement? = resolveItem(element, Namespace.STRUCT_FIELD)
}
