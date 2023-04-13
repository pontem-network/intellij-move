package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvPath

interface PathExpr: MvElement {
    val path: MvPath
}
