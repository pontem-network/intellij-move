package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructFieldRef
import org.move.lang.core.psi.MvStructFieldReferenceElement
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvReferenceBase
import org.move.lang.core.resolve.ref.MvReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveItem

class MvDotStructFieldAccessReferenceImpl(
    element: MvStructFieldReferenceElement
) : MvReferenceCached<MvStructFieldReferenceElement>(element) {

    override fun resolveInner(): MvNamedElement? = resolveItem(element, Namespace.DOT_ACCESSED_FIELD)
}

abstract class MvStructFieldRefMixin(node: ASTNode) : MvElementImpl(node),
                                                        MvStructFieldRef {
    override fun getReference(): MvReference {
        return MvDotStructFieldAccessReferenceImpl(this)
    }
}
