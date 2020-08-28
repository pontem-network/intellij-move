package org.move.lang.core.psi

import com.intellij.psi.PsiElement
import org.move.lang.core.resolve.ref.MoveReference

interface MoveReferenceElement : MoveElement {
    val referenceNameElement: PsiElement

    @JvmDefault
    val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): MoveReference
}

//interface MovePathReferenceElement : MoveReferenceElement {
//    override fun getReference(): Nv?
//
//    override val referenceNameElement: PsiElement
//
//    @JvmDefault
//    override val referenceName: String get() = referenceNameElement.unescapedText
//}

/**
 * Marks an element that has a reference.
 */
//interface MoveMandatoryReferenceElement : MoveReferenceElement {
//
////    override val referenceNameElement: PsiElement
//
////    @JvmDefault
////    override val referenceName: String get() = referenceNameElement.text
//
//    override fun getReference(): PsiReference
//}
