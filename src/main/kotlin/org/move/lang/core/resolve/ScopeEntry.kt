package org.move.lang.core.resolve

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.ResolutionContext
import org.move.lang.core.resolve.ref.RsPathResolveResult


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

fun List<ScopeEntry>.toPathResolveResults(ctx: ResolutionContext): List<RsPathResolveResult<MvElement>> {
    return this.map { toPathResolveResult(it, ctx) }
}

fun toPathResolveResult(scopeEntry: ScopeEntry, ctx: ResolutionContext): RsPathResolveResult<MvElement> {
    val element = scopeEntry.element
    val contextElement = ctx.methodOrPath
    if (contextElement != null) {
        return RsPathResolveResult(element, isVisibleInContext(scopeEntry, contextElement))
    } else {
        return RsPathResolveResult(element, true)
    }
}

