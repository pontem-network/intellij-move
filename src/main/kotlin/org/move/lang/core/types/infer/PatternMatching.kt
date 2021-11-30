package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.providedFields
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyTuple
import org.move.lang.core.types.ty.TyUnknown

fun collectBindings(pattern: MovePat, type: Ty): Map<MoveBindingPat, Ty> {
    val bindings = mutableMapOf<MoveBindingPat, Ty>()
    fun bind(pat: MovePat, ty: Ty) {
        when (pat) {
            is MoveBindingPat -> bindings += pat to ty
            is MoveTuplePat -> {
                if (ty is TyTuple && pat.patList.size == ty.types.size) {
                    pat.patList.zip(ty.types)
                        .forEach { (pat, ty) -> bind(pat, ty) }
                } else {
                    pat.patList.map { bind(it, TyUnknown) }
                }
            }
        }
    }
    bind(pattern, type)
    return bindings
}
