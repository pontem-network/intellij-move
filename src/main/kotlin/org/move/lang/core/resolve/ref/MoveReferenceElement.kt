package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.move.lang.core.psi.MvElement

interface PsiReferenceElement : PsiElement {
    val identifier: PsiElement?

    val referenceNameElement: PsiElement?
        get() = identifier

    val referenceName: String?
        get() = identifier?.text

    override fun getReference(): PsiReference?

    val unresolved: Boolean get() = reference?.resolve() == null
}

interface PsiMandatoryReferenceElement : PsiElement {
    val identifier: PsiElement

    val referenceNameElement: PsiElement
        get() = identifier

    val referenceName: String
        get() = identifier.text

    override fun getReference(): PsiReference
}

interface NamedAddressReferenceElement : PsiMandatoryReferenceElement {

    override fun getReference(): NamedAddressReference
}

interface MvReferenceElement : PsiReferenceElement, MvElement {

    override fun getReference(): MvPolyVariantReference?
}

interface MvMandatoryReferenceElement : MvReferenceElement {
    override val identifier: PsiElement

    override val referenceNameElement: PsiElement get() = identifier

    override val referenceName: String get() = referenceNameElement.text

    override fun getReference(): MvPolyVariantReference
}

//interface MvPathReferenceElement : MvReferenceElement {
//    override fun getReference(): MvPathReference?
//}

interface MvNameAccessChainReferenceElement : MvReferenceElement {
    override fun getReference(): MvPath2Reference?
}

//interface MvFQModuleReferenceElement : MvReferenceElement {
//    override fun getReference(): MvFQModuleReference?
//}

//interface MvStructPatFieldReferenceElement : MvMandatoryReferenceElement

interface MvStructFieldLitReferenceElement : MvMandatoryReferenceElement

interface MvSchemaRefFieldReferenceElement : MvMandatoryReferenceElement

interface MvItemSpecParameterReferenceElement: MvMandatoryReferenceElement
