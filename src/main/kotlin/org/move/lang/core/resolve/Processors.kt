package org.move.lang.core.resolve

import org.move.lang.core.psi.MoveNamedElement

data class SimpleScopeEntry(
    val name: String,
    val element: MoveNamedElement?
)

fun interface MatchingProcessor {
    fun match(entry: SimpleScopeEntry): Boolean

    fun match(name: String, elem: MoveNamedElement): Boolean {
        val entry = SimpleScopeEntry(name, elem)
        return match(entry)
    }

    fun match(elem: MoveNamedElement): Boolean {
        val name = elem.name ?: return false
        return match(name, elem)
    }

    fun matchAll(elements: Collection<MoveNamedElement>): Boolean =
        elements.any { match(it) }

    fun matchAllScopeEntriess(scopeEntries: Collection<SimpleScopeEntry>): Boolean =
        scopeEntries.any { match(it) }
}
