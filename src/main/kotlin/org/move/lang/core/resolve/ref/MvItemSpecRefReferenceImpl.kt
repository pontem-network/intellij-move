package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.allNonTestFunctions
import org.move.lang.core.psi.ext.itemSpec
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.structs
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.SimpleScopeEntry
import org.move.lang.core.resolve.collectResolveVariants
import org.move.lang.core.resolve.processAll

class MvItemSpecRefReferenceImpl(element: MvItemSpecRef): MvPolyVariantReferenceCached<MvItemSpecRef>(element) {

    override fun multiResolveInner(): List<MvNamedElement> =
        collectResolveVariants(element.referenceName) {
            processItemSpecRefResolveVariants(element, it)
        }
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
            it.name?.let { name -> SimpleScopeEntry(name, it, ALL_NAMESPACES) }
        }
    )
}
