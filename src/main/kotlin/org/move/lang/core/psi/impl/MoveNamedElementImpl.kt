package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvPsiFactory

abstract class MvNamedElementImpl(node: ASTNode) : MvElementImpl(node),
                                                     MvNamedElement {
    override fun getName(): String? = nameElement?.text

    override fun setName(name: String): PsiElement {
        nameElement?.replace(MvPsiFactory(project).createIdentifier(name))
        return this
    }

    override fun getNavigationElement(): PsiElement = nameElement ?: this

    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()
}
