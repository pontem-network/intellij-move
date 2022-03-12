package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement

data class SimpleScopeEntry(
    val name: String,
    val element: MvNamedElement
)

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

    fun matchAll(vararg collections: Collection<MvNamedElement>): Boolean =
        listOf(*collections).flatten().any { match(it) }
}
