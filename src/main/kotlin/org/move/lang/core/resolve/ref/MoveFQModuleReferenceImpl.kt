package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvFQModuleRef
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.processQualModuleRef

interface MvFQModuleReference : MvReference {
    override fun resolve(): MvNamedElement?
}

class MvFQModuleReferenceImpl(
    element: MvFQModuleRef,
) : MvReferenceCached<MvFQModuleRef>(element), MvFQModuleReference {

    override fun resolveInner(): MvNamedElement? {
        val referenceName = element.referenceName
        var resolved: MvModuleDef? = null
        processQualModuleRef(element) {
            val element = it.element as? MvModuleDef ?: return@processQualModuleRef false
            if (it.name == referenceName) {
                resolved = element
                true
            } else {
                false
            }
        }
        return resolved
    }
}
