package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.resolve.collectResolveVariants
import org.move.lang.core.resolve.processStructLitFieldResolveVariants

class MvStructLitFieldReferenceImpl(
    field: MvStructLitField
): MvPolyVariantReferenceCached<MvStructLitField>(field) {

    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    override fun multiResolveInner(): List<MvNamedElement> =
        collectResolveVariants(element.referenceName) {
            processStructLitFieldResolveVariants(element, false, it)
        }
}