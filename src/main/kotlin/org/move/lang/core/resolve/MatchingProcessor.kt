package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.isVisibleInContext

data class SimpleScopeEntry<T : MvNamedElement>(
    val name: String,
    val element: T
)

fun interface MatchingProcessor<T : MvNamedElement> {
    fun match(entry: SimpleScopeEntry<T>): Boolean

    fun match(contextVis: ItemVis, element: T): Boolean {
        if (!contextVis.isMsl && element.isMsl()) return false
        if (!element.isVisibleInContext(contextVis.itemScope)) return false

        val name = element.name ?: return false
        val entry = SimpleScopeEntry(name, element)
        return match(entry)
    }

    fun matchAll(itemVis: ItemVis, vararg collections: Iterable<T>): Boolean =
        sequenceOf(*collections)
            .flatten()
            .any { match(itemVis, it) }
}
