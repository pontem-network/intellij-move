package org.move.lang.core.resolve.ref

import org.move.lang.core.resolve.MvMandatoryReferenceElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.psi.MvStructPatField
import org.move.lang.core.resolve.resolveLocalItem

class MvStructFieldReferenceImpl(
    element: MvMandatoryReferenceElement
) : MvReferenceCached<MvMandatoryReferenceElement>(element) {

    override fun resolveInner() = resolveLocalItem(element, setOf(Namespace.STRUCT_FIELD))
}

class MvStructLitShorthandFieldReferenceImpl(
    element: MvStructLitField,
) : MvReferenceCached<MvStructLitField>(element) {

    override fun resolveInner(): List<MvNamedElement> {
        return listOf(
            resolveLocalItem(element, setOf(Namespace.STRUCT_FIELD)),
            resolveLocalItem(element, setOf(Namespace.NAME))
        ).flatten()
    }
}

class MvStructPatShorthandFieldReferenceImpl(
    element: MvStructPatField
) : MvReferenceCached<MvStructPatField>(element) {

    override fun resolveInner() = resolveLocalItem(element, setOf(Namespace.STRUCT_FIELD))
}
