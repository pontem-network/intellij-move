package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructDotField
import org.move.lang.core.resolve.MvStructFieldReferenceElement
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveLocalItem

class MvDotStructFieldAccessReferenceImpl(
    element: MvStructFieldReferenceElement
) : MvReferenceCached<MvStructFieldReferenceElement>(element) {

    override fun resolveInner(): List<MvNamedElement> = resolveLocalItem(element, setOf(Namespace.DOT_FIELD))
}

abstract class MvStructDotMixin(node: ASTNode) : MvElementImpl(node),
                                                 MvStructDotField {
    override fun getReference(): MvReference {
        return MvDotStructFieldAccessReferenceImpl(this)
    }
}
