package org.move.lang.core.types.infer

import org.move.lang.core.psi.MvType
import org.move.lang.core.psi.MvTypeParameter

/** Similar to [Substitution], but maps PSI to PSI instead of [Ty] to [Ty] */
class RsPsiSubstitution(
    val typeSubst: Map<MvTypeParameter, Value<MvType>> = emptyMap(),
) {
    sealed class Value<out P> {
        data object RequiredAbsent : Value<Nothing>()
        data object OptionalAbsent : Value<Nothing>()
        class Present<P>(val value: P) : Value<P>()
    }
}
