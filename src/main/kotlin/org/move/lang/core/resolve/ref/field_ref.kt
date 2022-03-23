package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvMandatoryReferenceElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.psi.MvStructPatField
import org.move.lang.core.resolve.resolveItem

class MvStructFieldReferenceImpl(
    element: MvMandatoryReferenceElement
) : MvReferenceCached<MvMandatoryReferenceElement>(element) {

    override fun resolveInner() = resolveItem(element, Namespace.STRUCT_FIELD)
}

class MvStructLitShorthandFieldReferenceImpl(
    element: MvStructLitField,
) : MvReferenceCached<MvStructLitField>(element) {

    override fun resolveInner(): List<MvNamedElement> {
        return listOf(
            resolveItem(element, Namespace.STRUCT_FIELD),
            resolveItem(element, Namespace.NAME)
        ).flatten()
    }
}

class MvStructPatShorthandFieldReferenceImpl(
    element: MvStructPatField
) : MvReferenceCached<MvStructPatField>(element) {

    override fun resolveInner() = resolveItem(element, Namespace.STRUCT_FIELD)
}
