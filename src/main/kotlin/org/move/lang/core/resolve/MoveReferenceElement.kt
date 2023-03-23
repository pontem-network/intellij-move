package org.move.lang.core.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.move.lang.core.psi.MvElement
import org.move.lang.core.resolve.ref.MvFQModuleReference
import org.move.lang.core.resolve.ref.MvPathReference
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.NamedAddressReference

interface PsiReferenceElement : PsiElement {
    val identifier: PsiElement?

    val referenceNameElement: PsiElement?
        get() = identifier

    val referenceName: String?
        get() = identifier?.text

    override fun getReference(): PsiReference?

    val unresolved: Boolean
        get() = reference?.resolve() == null

    val resolvable: Boolean get() = !this.unresolved
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

    override fun getReference(): MvReference?
}

interface MvMandatoryReferenceElement : MvReferenceElement {
    override val identifier: PsiElement

    override val referenceNameElement: PsiElement get() = identifier

    override val referenceName: String get() = referenceNameElement.text

    override fun getReference(): MvReference
}

interface MvPathReferenceElement : MvReferenceElement {
    override fun getReference(): MvPathReference?
}

interface MvFQModuleReferenceElement : MvReferenceElement {
    override fun getReference(): MvFQModuleReference?
}

interface MvStructFieldReferenceElement : MvMandatoryReferenceElement

interface MvStructFieldLitReferenceElement : MvMandatoryReferenceElement

interface MvSchemaRefFieldReferenceElement : MvMandatoryReferenceElement

interface MvItemSpecParameterReferenceElement: MvMandatoryReferenceElement
