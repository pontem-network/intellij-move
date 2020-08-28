package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.resolve.ResolveEngine

class MoveReferenceImpl(element: MoveReferenceElement) : MoveReferenceBase<MoveReferenceElement>(element) {

    override fun resolveVerbose(): ResolveEngine.ResolveResult =
        ResolveEngine.resolve(element)
}