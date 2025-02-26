package org.move.lang.core.resolve2

import org.move.ide.utils.imports.ImportCandidate
import org.move.lang.core.completion.providers.ImportCandidateScopeEntry
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.resolve.ScopeEntry
import org.move.lang.core.resolve.ScopeEntryWithVisibility
import org.move.lang.core.resolve.SimpleScopeEntry
import org.move.lang.core.resolve.ref.Namespace


data class ScopeEntry2(
    val name: String,
    val element: MvNamedElement,
    val namespaces: Set<Namespace>,
    val kind: ScopeEntryKind,
) {
    fun copyWithNs(namespaces: Set<Namespace>): ScopeEntry2 = this.copy(namespaces = namespaces)
    fun copyWithKind(kind: ScopeEntryKind) = this.copy(kind = kind)
}

sealed class ScopeEntryKind {
    class Simple: ScopeEntryKind()
    class WithVisibility(itemScope: NamedItemScope): ScopeEntryKind()
    class Import(val candidate: ImportCandidate): ScopeEntryKind()
}

fun ScopeEntry.asEntry2(): ScopeEntry2 {
    return when (this) {
        is SimpleScopeEntry -> this.asEntry2()
        is ScopeEntryWithVisibility -> this.asEntry2()
        is ImportCandidateScopeEntry -> this.asEntry2()
        else -> error("unreachable")
    }
}

fun SimpleScopeEntry.asEntry2(): ScopeEntry2 {
    return ScopeEntry2(
        this.name,
        this.element,
        this.namespaces,
        kind = ScopeEntryKind.Simple()
    )
}

fun ScopeEntryWithVisibility.asEntry2(): ScopeEntry2 {
    return ScopeEntry2(
        this.name,
        this.element,
        this.namespaces,
        kind = ScopeEntryKind.WithVisibility(itemScope = this.itemScope)
    )
}

fun ImportCandidateScopeEntry.asEntry2(): ScopeEntry2 {
    return ScopeEntry2(
        this.name,
        this.element,
        this.namespaces,
        kind = ScopeEntryKind.Import(this.candidate)
    )
}
