package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveNameIdentifierOwner
import org.move.lang.core.psi.MovePsiFactory

abstract class MoveNameIdentifierOwnerImpl(node: ASTNode) : MoveElementImpl(node),
                                                            MoveNameIdentifierOwner {
    protected open val nameElement: PsiElement?
        get() = findChildByType(MoveElementTypes.IDENTIFIER)

    override fun getName(): String? = nameElement?.text

    override fun setName(name: String): PsiElement {
        nameElement?.replace(MovePsiFactory(project).createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? = nameElement

    override fun getNavigationElement(): PsiElement = nameElement ?: this

    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()
}