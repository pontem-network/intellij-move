package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*

abstract class MvNamedElementImpl(node: ASTNode) : MvElementImpl(node),
                                                   MvNamedElement {
    override fun getName(): String? = nameElement?.text

    override fun setName(name: String): PsiElement {
        val newIdentifier = project.psiFactory.identifier(name)
        nameElement?.replace(newIdentifier)
        return this
    }

    override fun getNavigationElement(): PsiElement = nameElement ?: this

    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()

    override val fqName: String get() = "<unknown>"
}
