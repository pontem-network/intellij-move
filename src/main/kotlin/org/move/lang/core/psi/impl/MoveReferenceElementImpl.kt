package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.psi.ext.findFirstChildByType
import org.move.lang.core.resolve.ref.MoveReference

abstract class MoveReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                         MoveReferenceElement {
    abstract override fun getReference(): MoveReference
}
