package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvTypeArgument
import org.move.lang.core.psi.MvTypeArgumentList
import org.move.lang.core.resolve.ref.MvReferenceElement

val MvMethodOrPath.typeArguments: List<MvTypeArgument> get() = typeArgumentList?.typeArgumentList.orEmpty()

interface MvMethodOrPath: MvReferenceElement {
    val typeArgumentList: MvTypeArgumentList?
}