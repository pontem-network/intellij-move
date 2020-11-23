package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveFullyQualifiedModuleRef
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.resolve.processQualModuleRef

class MoveQualModuleReferenceImpl(
    element: MoveFullyQualifiedModuleRef,
) : MoveReferenceCached<MoveFullyQualifiedModuleRef>(element) {

    override fun resolveInner(): MoveNamedElement? {
        val referenceName = element.referenceName
        var resolved: MoveModuleDef? = null
        processQualModuleRef(element) {
            val element = it.element as? MoveModuleDef ?: return@processQualModuleRef false
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