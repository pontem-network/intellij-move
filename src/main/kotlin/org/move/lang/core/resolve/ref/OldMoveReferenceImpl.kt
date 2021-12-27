package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvReferenceElement
import org.move.lang.core.resolve.resolveItem

class OldMvReferenceImpl(
    element: MvReferenceElement,
    private val namespace: Namespace,
) : MvReferenceCached<MvReferenceElement>(element) {

    override fun resolveInner(): MvNamedElement? {
        return resolveItem(element, namespace)
    }
}
