package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
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

    override fun handleElementRename(newName: String): PsiElement {
        val psiFactory = element.project.psiFactory
        val newLitField = psiFactory.createStructLitField(newName, element.referenceName)
        element.replace(newLitField)
        return element
    }
}

class MvStructPatShorthandFieldReferenceImpl(
    element: MvStructPatField
) : MvReferenceCached<MvStructPatField>(element) {

    override fun resolveInner() = resolveItem(element, Namespace.STRUCT_FIELD)

    override fun handleElementRename(newName: String): PsiElement {
        val psiFactory = element.project.psiFactory
        val newPatField = psiFactory.createStructPatField(newName, element.referenceName)
        element.replace(newPatField)
        return element
//        }
    }
}
