package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.psi.ext.identifierNameElement
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl

abstract class MoveRefExprMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                 MoveRefExpr {
    override val referenceNameElement: PsiElement
        get() = qualifiedPath.identifierNameElement
}