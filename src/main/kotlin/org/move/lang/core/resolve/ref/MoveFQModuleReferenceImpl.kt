package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvFQModuleRef
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.processFQModuleRef
import org.move.stdext.wrapWithList

interface MvFQModuleReference : MvPolyVariantReference

class MvFQModuleReferenceImpl(
    element: MvFQModuleRef,
) : MvPolyVariantReferenceCached<MvFQModuleRef>(element), MvFQModuleReference {

    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    override fun multiResolveInner(): List<MvNamedElement> {
        val referenceName = element.referenceName ?: return emptyList()
        var resolved: MvModule? = null
        processFQModuleRef(element, referenceName) {
            if (it.name == referenceName) {
                resolved = it.element
                true
            } else {
                false
            }
        }
        return resolved.wrapWithList()
    }
}
