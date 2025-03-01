package org.move.lang.core.resolve.scopeEntry

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.resolve.isVisibleInContext
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.ResolutionContext
import org.move.lang.core.resolve.ref.RsPathResolveResult
import org.move.lang.core.resolve.ref.itemNs


data class ScopeEntry(
    val name: String,
    val element: MvNamedElement,
    val namespaces: Set<Namespace>,
    val customItemScope: NamedItemScope? = null,
) {
    fun copyWithNs(namespaces: Set<Namespace>): ScopeEntry = copy(namespaces = namespaces)
}

fun List<ScopeEntry>.filterByName(name: String): List<ScopeEntry> {
    return this.filter { it.name == name }
}

fun List<ScopeEntry>.namedElements(): List<MvNamedElement> = this.map { it.element }

fun List<ScopeEntry>.toPathResolveResults(contextElement: MvElement?): List<RsPathResolveResult> {
    return this.map { toPathResolveResult(it, contextElement) }
}

fun toPathResolveResult(scopeEntry: ScopeEntry, contextElement: MvElement?): RsPathResolveResult {
    val element = scopeEntry.element
    if (contextElement != null) {
        return RsPathResolveResult(element, isVisibleInContext(scopeEntry, contextElement))
    } else {
        return RsPathResolveResult(element, true)
    }
}

fun List<MvNamedElement>.asEntries(): List<ScopeEntry> {
    return this.mapNotNull { it.asEntry() }
}

fun MvNamedElement.asEntry(): ScopeEntry? {
    val name = this.name ?: return null
    return ScopeEntry(name, this, this.itemNs)
}

