package org.move.lang.core.resolve2.ref

import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.collectResolveVariants
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve2.processBindingPatResolveVariants

class MvBindingPatReferenceImpl(
    element: MvBindingPat
): MvPolyVariantReferenceCached<MvBindingPat>(element) {

    override fun multiResolveInner(): List<MvNamedElement> =
        collectResolveVariants(element.referenceName) {
            processBindingPatResolveVariants(element, false, it)
        }

}