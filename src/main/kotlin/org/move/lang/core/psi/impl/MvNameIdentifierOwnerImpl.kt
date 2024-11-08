package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvMandatoryNameIdentifierOwner
import org.move.lang.core.psi.MvNameIdentifierOwner

abstract class MvNameIdentifierOwnerImpl(node: ASTNode) : MvNamedElementImpl(node),
                                                          MvNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = nameElement

    override fun getPresentation(): ItemPresentation? = org.move.ide.presentation.getPresentation(this)
}

abstract class MvMandatoryNameIdentifierOwnerImpl(node: ASTNode): MvMandatoryNamedElementImpl(node),
                                                                  MvMandatoryNameIdentifierOwner {

    override fun getNameIdentifier(): PsiElement = nameElement

    override fun getPresentation(): ItemPresentation? = org.move.ide.presentation.getPresentation(this)
                                                                  }
