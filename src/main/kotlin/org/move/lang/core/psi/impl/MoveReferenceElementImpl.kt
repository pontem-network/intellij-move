package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl
import org.move.lang.core.resolve.ref.MoveReferenceKind

abstract class MoveReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                         MoveReferenceElement {
    override val referenceNameElement: PsiElement
        get() = findNotNullChildByType(IDENTIFIER)

    override fun getReference(): MoveReference =
        MoveReferenceImpl(this, MoveReferenceKind.NAME)
}