package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MovePathExpr
import org.move.lang.core.psi.impl.MoveElementImpl
import org.move.lang.core.resolve.ref.MoveReference

interface MovePathExprReference : MoveReference

class MovePathExprReferenceImpl(element: MovePathExpr) : PsiReferenceBase<MovePathExpr>(element),
                                                         MovePathExprReference {
    override fun resolve(): MoveNamedElement? = null
}

abstract class MovePathExprImplMixin(node: ASTNode) : MoveElementImpl(node),
                                                      MoveReferenceElement,
                                                      MovePathExpr {
    override val referenceNameElement: PsiElement?
        get() = null

    override fun getReference(): MovePathExprReference? {
        return MovePathExprReferenceImpl(this)
    }
}