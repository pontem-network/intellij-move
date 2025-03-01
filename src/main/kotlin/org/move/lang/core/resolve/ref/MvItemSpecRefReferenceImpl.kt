package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.allNonTestFunctions
import org.move.lang.core.psi.ext.itemSpec
import org.move.lang.core.psi.ext.module
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntries
import org.move.lang.core.resolve.scopeEntry.filterByName

class MvItemSpecRefReferenceImpl(element: MvItemSpecRef): MvPolyVariantReferenceCached<MvItemSpecRef>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val entries =
            getVerifiableItemEntries(element).filterByName(element.referenceName)
        return entries.map { it.element }
    }
}

fun getVerifiableItemEntries(itemSpecRef: MvItemSpecRef): List<ScopeEntry> {
    val module = itemSpecRef.itemSpec.module ?: return emptyList()
    val verifiableItems = buildList {
        addAll(module.allNonTestFunctions())
        addAll(module.structList)
        addAll(module.enumList)
    }
    return verifiableItems.asEntries()
}
