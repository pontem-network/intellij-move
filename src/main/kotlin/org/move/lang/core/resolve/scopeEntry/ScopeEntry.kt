package org.move.lang.core.resolve.scopeEntry

import org.move.lang.core.completion.LOCAL_ITEM_PRIORITY
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.completionPriority
import org.move.lang.core.resolve.isVisibleInContext
import org.move.lang.core.resolve.ref.NsSet
import org.move.lang.core.resolve.ref.RsPathResolveResult
import org.move.lang.core.resolve.ref.itemNs


data class ScopeEntry(
    val name: String,
    private val element: Lazy<MvNamedElement?>,
    val namespaces: NsSet,
    val customItemScope: NamedItemScope? = null,
) {
    fun element(): MvNamedElement? = this.element.value

    val completionPriority: Double get() = this.element()?.completionPriority ?: LOCAL_ITEM_PRIORITY

    fun copyWithNs(ns: NsSet): ScopeEntry = copy(namespaces = ns)
}

fun List<ScopeEntry>.filterByName(name: String): List<ScopeEntry> {
    return this.filter { it.name == name }
}

fun List<ScopeEntry>.namedElements(): List<MvNamedElement> = this.mapNotNull { it.element() }

fun List<ScopeEntry>.singleItemOrNull(): MvNamedElement? = this.singleOrNull()?.element()

fun List<ScopeEntry>.toPathResolveResults(contextElement: MvElement?): List<RsPathResolveResult> {
    return this.mapNotNull { toPathResolveResult(it, contextElement) }
}

fun toPathResolveResult(scopeEntry: ScopeEntry, contextElement: MvElement?): RsPathResolveResult? {
    val element = scopeEntry.element() ?: return null
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
    return ScopeEntry(name, lazy { this }, this.itemNs)
}

