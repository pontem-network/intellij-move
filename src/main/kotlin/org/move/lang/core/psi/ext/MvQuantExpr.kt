package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

interface MvQuantBindingsOwner: MvElement {
    val quantBindings: MvQuantBindings?
}

interface MvQuantExpr : MvQuantBindingsOwner {
    val expr: MvExpr?
    val quantWhere: MvQuantWhere?
}

val MvQuantBindingsOwner.bindings: List<MvPatBinding>
    get() = quantBindings?.quantBindingList.orEmpty().mapNotNull { it.binding }

val MvQuantBinding.binding: MvPatBinding
    get() = when (this) {
        is MvTypeQuantBinding -> this.patBinding
        is MvRangeQuantBinding -> this.patBinding
        else -> error("unreachable")
    }
