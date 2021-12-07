package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement

//sealed class Resolution

data class SimpleScopeEntry(
    val name: String,
    val element: MvNamedElement?
)

//class Stop: Resolution()

fun interface MatchingProcessor {
    fun match(entry: SimpleScopeEntry): Boolean

    fun match(name: String, elem: MvNamedElement): Boolean {
        val entry = SimpleScopeEntry(name, elem)
        return match(entry)
    }

    fun match(element: MvNamedElement): Boolean {
        val name = element.name ?: return false
        return match(name, element)
    }

    fun matchAll(elements: Collection<MvNamedElement>): Boolean =
        elements.any { match(it) }

    fun matchAllScopeEntriess(scopeEntries: Collection<SimpleScopeEntry>): Boolean =
        scopeEntries.any { match(it) }
}
