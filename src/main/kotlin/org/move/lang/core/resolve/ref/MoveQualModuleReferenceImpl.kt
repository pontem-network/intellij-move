package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveFullyQualifiedModuleRef
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.resolve.resolveQualModuleRef

class MoveQualModuleReferenceImpl(
    element: MoveFullyQualifiedModuleRef,
) : MoveReferenceCached<MoveFullyQualifiedModuleRef>(element) {

    override fun resolveInner(): MoveNamedElement? {
        return resolveQualModuleRef(element)
    }
}