package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveCallExpr
import org.move.lang.core.psi.ext.identifierNameElement
import org.move.lang.core.psi.impl.MoveReferenceElementImpl

abstract class MoveCallExprMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                  MoveCallExpr {
    override val referenceNameElement: PsiElement
        get() = qualifiedPath.identifierNameElement
}