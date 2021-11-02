package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveFQModuleRef
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.resolve.processQualModuleRef

interface MoveFQModuleReference : MoveReference {
    override fun resolve(): MoveModuleDef?
}

class MoveFQModuleReferenceImpl(
    element: MoveFQModuleRef,
) : MoveReferenceBase<MoveFQModuleRef>(element), MoveFQModuleReference {

    override fun resolve(): MoveModuleDef? {
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
