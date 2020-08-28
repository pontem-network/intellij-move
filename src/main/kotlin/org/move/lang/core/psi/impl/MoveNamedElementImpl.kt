package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveNamedElement

abstract class MoveNamedElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                     MoveNamedElement {
    protected open val nameElement: PsiElement?
        get() = findChildByType(MoveElementTypes.IDENTIFIER)

    override fun getName(): String? = nameElement?.text

    override fun setName(name: String): PsiElement {
        throw UnsupportedOperationException("Unsupported yet")
    }

    override fun getNavigationElement(): PsiElement = nameElement ?: this

    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()
}