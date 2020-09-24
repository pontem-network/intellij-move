package org.move.lang.core.psi.ref_element

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveElement
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
