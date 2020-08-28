package org.move.lang.core.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElement
import org.move.lang.core.resolve.ref.MoveReference

abstract class MoveElementImpl(node: ASTNode) : ASTWrapperPsiElement(node),
                                                MoveElement {
    override fun getReference(): MoveReference? = null
}