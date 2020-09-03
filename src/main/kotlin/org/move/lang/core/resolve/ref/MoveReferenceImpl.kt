package org.move.lang.core.resolve.ref

import org.move.lang.core.completion.CompletionEngine
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.resolve.ResolveEngine

class MoveReferenceImpl(
    element: MoveReferenceElement,
    private val kind: MoveReferenceKind
) : MoveReferenceBase<MoveReferenceElement>(element) {

    override fun resolveVerbose(): ResolveEngine.ResolveResult =
        ResolveEngine.resolve(element, kind)

    override fun getVariants(): Array<out Any> = CompletionEngine.complete(element, kind)
}