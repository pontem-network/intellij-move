package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.itemScope
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.mslLetScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.resolveLocalItem
import org.move.stdext.chain
import org.move.stdext.wrapWithList

class MvItemSpecRefReferenceImpl(element: MvItemSpecRef) : MvPolyVariantReferenceCached<MvItemSpecRef>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val module = element.itemSpec.module ?: return emptyList()
        val referenceName = element.referenceName
        return module.mslSpecifiableItems
            .filter { it.name == referenceName }
    }

}
