package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveNameIdentifierOwner

abstract class MoveNameIdentifierOwnerImpl(node: ASTNode) : MoveNamedIdentifierOwnerImpl(node),
                                                            MoveNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = nameElement
}