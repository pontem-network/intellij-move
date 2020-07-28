package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

/**
 * Provides basic methods for reference implementation. This interface should not be used in any analysis.
 */
//interface MvReferenceElementBase : PsiElement {
//    val referenceNameElement: PsiElement?
//
//    @JvmDefault
//    val referenceName: String? get() = referenceNameElement?.text
//}

/**
 * Marks an element that optionally can have a reference.
 */
interface MoveReferenceElement : PsiElement {
    val referenceNameElement: PsiElement?

    @JvmDefault
    val referenceName: String?
        get() = referenceNameElement?.text

    override fun getReference(): PsiReference?
}

//interface MvPathReferenceElement : MvReferenceElement {
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
interface MoveMandatoryReferenceElement : MoveReferenceElement {

//    override val referenceNameElement: PsiElement

//    @JvmDefault
//    override val referenceName: String get() = referenceNameElement.text

    override fun getReference(): PsiReference
}
