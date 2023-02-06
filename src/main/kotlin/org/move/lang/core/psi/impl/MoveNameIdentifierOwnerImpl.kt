package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvNameIdentifierOwner

abstract class MvNameIdentifierOwnerImpl(node: ASTNode) : MvNamedElementImpl(node),
                                                          MvNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = nameElement
}
