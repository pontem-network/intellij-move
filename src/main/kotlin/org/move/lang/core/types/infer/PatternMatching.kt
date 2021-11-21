package org.move.lang.core.types.infer

import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MovePat
import org.move.lang.core.types.ty.Ty

fun collectBindings(pattern: MovePat, type: Ty): Map<String, Ty> {
    val bindings = mutableMapOf<String, Ty>()
    fun go(pat: MovePat, type: Ty) {
        when (pat) {
            is MoveBindingPat -> bindings += pat.name!! to type
        }
    }
    go(pattern, type)
    return bindings
}
