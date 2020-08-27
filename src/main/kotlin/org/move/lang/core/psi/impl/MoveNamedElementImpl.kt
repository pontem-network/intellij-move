package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveNameIdentifierOwner

abstract class MoveNamedElementImpl(node: ASTNode) : MoveElementImpl(node), MoveNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = findChildByType(MoveElementTypes.IDENTIFIER)

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        TODO("Not yet implemented")
    }
}