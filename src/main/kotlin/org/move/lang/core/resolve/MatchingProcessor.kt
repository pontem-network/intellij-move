package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.isVisibleInContext

data class ScopeItem<T : MvNamedElement>(
    val name: String,
    val element: T
)

fun interface MatchingProcessor<T : MvNamedElement> {
    fun match(entry: ScopeItem<T>): Boolean

    fun match(element: T): Boolean {
        val name = element.name ?: return false
        val entry = ScopeItem(name, element)
        return match(entry)
    }

    fun match(contextVis: ItemVis, element: T): Boolean {
        if (!contextVis.isMsl && element.isMsl()) return false
        if (!element.isVisibleInContext(contextVis.itemScopes)) return false

        val name = element.name ?: return false
        val entry = ScopeItem(name, element)
        return match(entry)
    }

    fun matchAll(itemVis: ItemVis, vararg collections: Iterable<T>): Boolean =
        sequenceOf(*collections)
            .flatten()
            .any { match(itemVis, it) }

    fun matchAll(vararg collections: Iterable<T>): Boolean =
        sequenceOf(*collections)
            .flatten()
            .any { match(it) }
}
