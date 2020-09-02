package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveCallExpr
import org.move.lang.core.psi.ext.identifierNameElement
import org.move.lang.core.psi.impl.MoveElementImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl

abstract class MoveCallExprMixin(node: ASTNode) : MoveElementImpl(node),
                                                  MoveCallExpr {
    override val referenceNameElement: PsiElement
        get() = qualifiedPath.identifierNameElement

    override fun getReference(): MoveReference = MoveReferenceImpl(this)
}