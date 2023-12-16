package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.resolveLocalItem

class MvItemSpecRefReferenceImpl(element: MvItemSpecRef) : MvPolyVariantReferenceCached<MvItemSpecRef>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        return resolveLocalItem(element, setOf(Namespace.SPEC_ITEM))
    }

}
