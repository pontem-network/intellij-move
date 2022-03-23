package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

interface MvQuantBindingsOwner {
    val quantBindings: MvQuantBindings?
}

val MvQuantBindingsOwner.bindings: List<MvBindingPat>
    get() = quantBindings?.quantBindingList.orEmpty().mapNotNull { it.bindingPat }

val MvQuantBinding.bindingPat: MvBindingPat?
    get() = when (this) {
        is MvTypeQuantBinding -> this.bindingPat
        is MvRangeQuantBinding -> this.bindingPat
        else -> null
    }
