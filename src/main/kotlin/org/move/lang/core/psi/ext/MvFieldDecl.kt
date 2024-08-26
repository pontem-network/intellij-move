package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvType

interface MvFieldDecl: MvDocAndAttributeOwner {
    val type: MvType?
}