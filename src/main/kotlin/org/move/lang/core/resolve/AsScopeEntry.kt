package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.ref.itemNs

fun List<MvNamedElement>.asEntries(): List<ScopeEntry> {
    return this.mapNotNull { it.asEntry() }
}

fun MvNamedElement.asEntry(): ScopeEntry? {
    val name = this.name ?: return null
    return ScopeEntry(name, this, this.itemNs)
}