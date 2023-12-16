package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.psi.MvStructPatField
import org.move.lang.core.resolve.resolveLocalItem

class MvStructFieldReferenceImpl(
    element: MvMandatoryReferenceElement
) : MvPolyVariantReferenceCached<MvMandatoryReferenceElement>(element) {

    override fun multiResolveInner() = resolveLocalItem(element, setOf(Namespace.STRUCT_FIELD))
}

class MvStructLitShorthandFieldReferenceImpl(
    element: MvStructLitField,
) : MvPolyVariantReferenceCached<MvStructLitField>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        return listOf(
            resolveLocalItem(element, setOf(Namespace.STRUCT_FIELD)),
            resolveLocalItem(element, setOf(Namespace.NAME))
        ).flatten()
    }
}

class MvStructPatShorthandFieldReferenceImpl(
    element: MvStructPatField
) : MvPolyVariantReferenceCached<MvStructPatField>(element) {

    override fun multiResolveInner() = resolveLocalItem(element, setOf(Namespace.STRUCT_FIELD))
}
