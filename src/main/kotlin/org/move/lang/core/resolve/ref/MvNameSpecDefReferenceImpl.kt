package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.resolveItem

class MvNameSpecDefReferenceImpl(element: MvItemSpec): MvReferenceCached<MvItemSpec>(element) {

    override fun resolveInner(): List<MvNamedElement> = resolveItem(element, setOf(Namespace.SPEC_ITEM))

}
