package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.move.lang.core.psi.MovePathExpr

interface MovePathExprReference : PsiReference

class MovePathExprReferenceImpl(element: MovePathExpr) : PsiReferenceBase<MovePathExpr>(element),
    MovePathExprReference {
    override fun resolve(): PsiElement? {
        return null
    }
}

abstract class MovePathExprImplMixin(node: ASTNode) : MoveElementImpl(node), MoveReferenceElement, MovePathExpr {
    override val referenceNameElement: PsiElement?
        get() = null

    override fun getReference(): MovePathExprReference? {
        return MovePathExprReferenceImpl(this)
    }
}