package org.move.lang.core.resolve.scopeEntry

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.completionPriority
import org.move.lang.core.resolve.isVisibleInContext
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.RsPathResolveResult
import org.move.lang.core.resolve.ref.itemNs
import org.move.lang.core.types.ItemFQName
import org.move.lang.core.types.fqName


data class ScopeEntry(
    val name: String,
    private val element: MvNamedElement,
    val namespaces: Set<Namespace>,
    val customItemScope: NamedItemScope? = null,
) {
    fun copyWithNs(namespaces: Set<Namespace>): ScopeEntry = copy(namespaces = namespaces)

    fun element(): MvNamedElement = this.element
    fun elementFQName(): ItemFQName? = this.element.fqName()

    val completionPriority: Double get() = this.element().completionPriority
}

fun List<ScopeEntry>.filterByName(name: String): List<ScopeEntry> {
    return this.filter { it.name == name }
}

fun List<ScopeEntry>.namedElements(): List<MvNamedElement> = this.map { it.element() }

fun List<ScopeEntry>.singleItemOrNull(): MvNamedElement? = this.singleOrNull()?.element()

fun List<ScopeEntry>.toPathResolveResults(contextElement: MvElement?): List<RsPathResolveResult> {
    return this.map { toPathResolveResult(it, contextElement) }
}

fun toPathResolveResult(scopeEntry: ScopeEntry, contextElement: MvElement?): RsPathResolveResult {
    val element = scopeEntry.element()
    return if (contextElement != null) {
        RsPathResolveResult(element, isVisibleInContext(scopeEntry, contextElement))
    } else {
        RsPathResolveResult(element, true)
    }
}

fun List<MvNamedElement>.asEntries(): List<ScopeEntry> {
    return this.mapNotNull { it.asEntry() }
}

fun MvNamedElement.asEntry(): ScopeEntry? {
    val name = this.name ?: return null
    return ScopeEntry(name, this, this.itemNs)
}

