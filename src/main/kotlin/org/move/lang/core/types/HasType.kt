package org.move.lang.core.types

import org.move.lang.core.psi.MoveElement

interface HasResolvedType: MoveElement {
    fun resolvedType(): BaseType?
}
