package org.move.lang.core.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.resolve.ref.MoveFQModuleReference
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

    override fun getReference(): NamedAddressReference?
}

interface MoveReferenceElement : PsiReferenceElement, MoveElement {

    override fun getReference(): MoveReference?
}

interface MoveFQModuleReferenceElement: MoveReferenceElement {
    override fun getReference(): MoveFQModuleReference?
}

interface MoveSchemaReferenceElement : MoveReferenceElement

interface MoveStructFieldReferenceElement : MoveReferenceElement
