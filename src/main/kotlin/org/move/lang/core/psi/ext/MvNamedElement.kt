package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import org.move.lang.MvElementTypes

interface MvNamedElement : PsiNamedElement, NavigatablePsiElement

interface MvNameIdentifierOwner : MvNamedElement, PsiNameIdentifierOwner

abstract class MvNamedElementImpl(node: ASTNode) : MoveElementImpl(node), MvNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = findChildByType(MvElementTypes.IDENTIFIER)

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        TODO("Not yet implemented")
    }
}