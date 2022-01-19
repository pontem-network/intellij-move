package org.move.lang.core.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.move.lang.core.resolve.ref.*

interface PsiReferenceElement : PsiElement {
    val identifier: PsiElement?

    val referenceNameElement: PsiElement?
        get() = identifier

    val referenceName: String?
        get() = identifier?.text

    override fun getReference(): PsiReference?

    val isUnresolved: Boolean
        get() = reference?.resolve() == null
}

interface NamedAddressReferenceElement : PsiReferenceElement {

    override fun getReference(): NamedAddressReference
}

interface MvReferenceElement : PsiReferenceElement, MvElement {

    override fun getReference(): MvReference?
}

interface MvMandatoryReferenceElement: MvReferenceElement {
    override val identifier: PsiElement

    override val referenceNameElement: PsiElement get() = identifier

    override val referenceName: String get() = referenceNameElement.text

    override fun getReference(): MvReference
}

//interface MvPolyVariantReferenceElement: PsiReferenceElement, MvElement {
//    override val identifier: PsiElement
//
////    override val referenceNameElement: PsiElement get() = identifier
//
////    override val referenceName: String get() = referenceNameElement.text
//
//    override fun getReference(): MvReference
//}

interface MvPathReferenceElement: MvReferenceElement {
    override fun getReference(): MvPathReference?
}

interface MvFQModuleReferenceElement: MvReferenceElement {
    override fun getReference(): MvFQModuleReference?
}

interface MvStructFieldReferenceElement : MvMandatoryReferenceElement

interface MvStructFieldLitReferenceElement: MvMandatoryReferenceElement
