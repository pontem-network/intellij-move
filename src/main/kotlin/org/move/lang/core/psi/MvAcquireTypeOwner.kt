package org.move.lang.core.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor

interface MvAcquireTypesOwner: MvElement

fun visitInnerAcquireTypeOwners(element: MvElement, visit: (MvAcquireTypesOwner) -> Unit) {
    val recursiveVisitor = object: PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is MvAcquireTypesOwner) {
                visit(element)
            }
            super.visitElement(element)
        }
    }
    recursiveVisitor.visitElement(element)
}
