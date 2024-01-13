package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.isMslOnlyItem
import org.move.lang.core.psi.isVisibleInContext

data class ScopeItem<T: MvNamedElement>(
    val name: String,
    val element: T
)

fun interface MatchingProcessor<T: MvNamedElement> {
    fun match(entry: ScopeItem<T>): Boolean

    fun match(itemElement: T): Boolean {
        val name = itemElement.name ?: return false
        val entry = ScopeItem(name, itemElement)
        return match(entry)
    }

    fun match(contextScopeInfo: ContextScopeInfo, itemElement: T): Boolean {
        if (
            !contextScopeInfo.isMslScope && itemElement.isMslOnlyItem
        ) return false
        if (!itemElement.isVisibleInContext(contextScopeInfo.refItemScopes)) return false

        val name = itemElement.name ?: return false
        val entry = ScopeItem(name, itemElement)
        return match(entry)
    }

    fun matchAll(contextScopeInfo: ContextScopeInfo, vararg collections: Iterable<T>): Boolean =
        sequenceOf(*collections)
            .flatten()
            .any { match(contextScopeInfo, it) }

    fun matchAll(vararg collections: Iterable<T>): Boolean =
        sequenceOf(*collections)
            .flatten()
            .any { match(it) }
}
