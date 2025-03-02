package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.resolve.scopeEntry.filterByName
import org.move.lang.core.resolve.getStructLitFieldResolveVariants
import org.move.lang.core.resolve.scopeEntry.namedElements

class MvStructLitFieldReferenceImpl(
    field: MvStructLitField
): MvPolyVariantReferenceCached<MvStructLitField>(field) {

    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    override fun multiResolveInner(): List<MvNamedElement> =
        getStructLitFieldResolveVariants(element, false)
            .filterByName(element.referenceName)
            .namedElements()
}