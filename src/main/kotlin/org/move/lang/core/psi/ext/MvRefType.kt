package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvRefType

val MvRefType.mutable: Boolean
    get() =
        "mut" in this.refTypeStart.text
