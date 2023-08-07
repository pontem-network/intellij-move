package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvPathType
import org.move.lang.core.psi.MvRefType
import org.move.lang.core.psi.MvType
import org.move.lang.core.psi.MvTypeArgument
import org.move.lang.core.resolve.ref.MvPolyVariantReference

val MvType.moveReference: MvPolyVariantReference?
    get() = when (this) {
        is MvPathType -> this.path.reference
        is MvRefType -> this.type?.moveReference
        else -> null
    }
val MvType.typeArguments: List<MvTypeArgument>
    get() {
        return when (this) {
            is MvPathType -> this.path.typeArguments
            is MvRefType -> this.type?.typeArguments.orEmpty()
            else -> emptyList()
        }
    }
