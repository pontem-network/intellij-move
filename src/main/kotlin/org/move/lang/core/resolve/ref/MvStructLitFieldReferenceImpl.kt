package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.resolve.filterByName
import org.move.lang.core.resolve.getStructLitFieldResolveVariants

class MvStructLitFieldReferenceImpl(
    field: MvStructLitField
): MvPolyVariantReferenceCached<MvStructLitField>(field) {

    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    override fun multiResolveInner(): List<MvNamedElement> =
        getStructLitFieldResolveVariants(element, false)
            .filterByName(element.referenceName)
            .map { it.element }
}