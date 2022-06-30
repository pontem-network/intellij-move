package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement

data class SimpleScopeEntry(
    val name: String,
    val element: MvNamedElement
)

fun interface MatchingProcessor {
    fun match(entry: SimpleScopeEntry): Boolean

    fun match(itemVis: ItemVis, element: MvNamedElement): Boolean {
        if (!element.isVisibleInScopes(itemVis)) return false
        val name = element.name ?: return false
        val entry = SimpleScopeEntry(name, element)
        return match(entry)
    }

    fun matchAll(itemVis: ItemVis, vararg collections: Iterable<MvNamedElement>): Boolean =
        listOf(*collections)
            .flatten()
            .any { match(itemVis, it) }

//    private fun match(element: MvNamedElement): Boolean {
//        val name = element.name ?: return false
//        val entry = SimpleScopeEntry(name, element)
//        return match(entry)
//    }
}
