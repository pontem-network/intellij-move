package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement

data class ScopeItem<T: MvNamedElement>(
    val name: String,
    val element: T
)
