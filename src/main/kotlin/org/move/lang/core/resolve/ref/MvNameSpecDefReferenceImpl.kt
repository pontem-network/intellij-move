package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.resolveLocalItem

class MvNameSpecDefReferenceImpl(element: MvItemSpec): MvReferenceCached<MvItemSpec>(element) {

    override fun resolveInner(): List<MvNamedElement> = resolveLocalItem(element, setOf(Namespace.SPEC_ITEM))

}
