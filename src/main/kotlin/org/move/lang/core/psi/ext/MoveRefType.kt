package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveRefType

val MoveRefType.mutable: Boolean
    get() =
        "mut" in this.refTypeStart.text
