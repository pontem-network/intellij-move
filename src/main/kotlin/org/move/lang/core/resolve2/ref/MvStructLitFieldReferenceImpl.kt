package org.move.lang.core.resolve2.ref

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.resolve.collectResolveVariants
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.ResolveCacheDependency
import org.move.lang.core.resolve2.processStructLitFieldResolveVariants

class MvStructLitFieldReferenceImpl(
    field: MvStructLitField
): MvPolyVariantReferenceCached<MvStructLitField>(field) {

    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    override fun multiResolveInner(): List<MvNamedElement> =
        collectResolveVariants(element.referenceName) {
            processStructLitFieldResolveVariants(element, false, it)
        }
}