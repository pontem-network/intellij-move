package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvNameSpecDef
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.resolveItem

class MvNameSpecDefReferenceImpl(element: MvNameSpecDef): MvReferenceCached<MvNameSpecDef>(element) {

    override fun resolveInner(): List<MvNamedElement> = resolveItem(element, Namespace.SPEC_ITEM)

}
