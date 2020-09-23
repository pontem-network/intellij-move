package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MovePsiFactory

abstract class MoveNamedElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                     MoveNamedElement {
    override fun getName(): String? = nameElement?.text

    override fun setName(name: String): PsiElement {
        nameElement?.replace(MovePsiFactory(project).createIdentifier(name))
        return this
    }

    override fun getNavigationElement(): PsiElement = nameElement ?: this

    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()
}