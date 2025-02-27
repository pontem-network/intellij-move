package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.allNonTestFunctions
import org.move.lang.core.psi.ext.itemSpec
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.structs
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.ScopeEntry
import org.move.lang.core.resolve.asEntries
import org.move.lang.core.resolve.filterByName

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
        addAll(module.structs())
        addAll(module.enumList)
    }
    return verifiableItems.asEntries()
}

fun processItemSpecRefResolveVariants(
    itemSpecRef: MvItemSpecRef,
    processor: RsResolveProcessor
): Boolean {
    val module = itemSpecRef.itemSpec.module ?: return false
    val mslEnabledItems =
        listOf(
            module.allNonTestFunctions(),
            module.structs(),
            module.enumList,
        ).flatten()
    return processor.processAll(
        mslEnabledItems.mapNotNull {
            it.name?.let { name -> ScopeEntry(name, it, ALL_NS) }
        }
    )
}
