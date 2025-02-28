package org.move.lang.core.psi.ext.label

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvLabelDecl
import org.move.lang.core.psi.psiFactory

abstract class MvLabelDeclMixin(node: ASTNode): MvElementImpl(node),
                                                MvLabelDecl {

    override fun getNameIdentifier(): PsiElement = quoteIdentifier

    override fun getName(): String? = nameIdentifier.text

    override fun setName(name: String): PsiElement {
        nameIdentifier.replace(project.psiFactory.quoteIdentifier(name))
        return this
    }
}