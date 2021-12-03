package org.move.lang.core.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.move.lang.core.resolve.ref.MoveFQModuleReference
import org.move.lang.core.resolve.ref.MovePathReference
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.NamedAddressReference

interface PsiReferenceElement : PsiElement {
    val identifier: PsiElement?

    @JvmDefault
    val referenceNameElement: PsiElement?
        get() = identifier

    @JvmDefault
    val referenceName: String?
        get() = identifier?.text

    override fun getReference(): PsiReference?

    @JvmDefault
    val isUnresolved: Boolean
        get() = reference?.resolve() == null
}

interface NamedAddressReferenceElement : PsiReferenceElement {

    override fun getReference(): NamedAddressReference
}

interface MoveReferenceElement : PsiReferenceElement, MoveElement {

    override fun getReference(): MoveReference?
}

interface MoveMandatoryReferenceElement: MoveReferenceElement {
    override val identifier: PsiElement

    @JvmDefault
    override val referenceNameElement: PsiElement get() = identifier

    @JvmDefault
    override val referenceName: String get() = referenceNameElement.text

    override fun getReference(): MoveReference
}

interface MovePathReferenceElement: MoveReferenceElement {
    override fun getReference(): MovePathReference?
}

interface MoveFQModuleReferenceElement: MoveReferenceElement {
    override fun getReference(): MoveFQModuleReference?
}

interface MoveStructFieldReferenceElement : MoveMandatoryReferenceElement
