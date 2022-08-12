package org.move.lang.core.psi.ext

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.core.psi.MvPathType
import org.move.lang.core.psi.MvRefType
import org.move.lang.core.psi.MvType
import org.move.lang.core.psi.MvTypeArgument
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.types.infer.inferTypeTy
import org.move.lang.core.types.ty.Ty

fun MvType.ty(): Ty = getProjectPsiDependentCache(this) { inferTypeTy(it, it.isMsl()) }

val MvType.moveReference: MvReference?
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
