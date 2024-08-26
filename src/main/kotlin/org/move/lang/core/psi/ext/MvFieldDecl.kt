package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvType

val MvFieldDecl.owner: MvFieldsOwner? get() = ancestorStrict()

interface MvFieldDecl: MvDocAndAttributeOwner {
    val type: MvType?
}