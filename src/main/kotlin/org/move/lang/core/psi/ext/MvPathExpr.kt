package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.move.lang.core.psi.MvPathExpr

interface MvPathExprReference : PsiReference

class MvPathExprReferenceImpl(element: MvPathExpr) : PsiReferenceBase<MvPathExpr>(element),
    MvPathExprReference {
    override fun resolve(): PsiElement? {
        return null
    }
}

abstract class MvPathExprImplMixin(node: ASTNode) : MoveElementImpl(node), MoveReferenceElement, MvPathExpr {
    override val referenceNameElement: PsiElement?
        get() = null

    override fun getReference(): MvPathExprReference? {
        return MvPathExprReferenceImpl(this)
    }
}