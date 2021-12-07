package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvReferenceElement
import org.move.lang.core.psi.ext.findFirstChildByType
import org.move.lang.core.resolve.ref.MvReference

abstract class MvReferenceElementImpl(node: ASTNode) : MvElementImpl(node),
                                                         MvReferenceElement {
    abstract override fun getReference(): MvReference
}
