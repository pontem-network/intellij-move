package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveStructPat
import org.move.lang.core.psi.impl.MoveTypeReferenceElementImpl

abstract class MoveStructPatMixin(node: ASTNode) : MoveTypeReferenceElementImpl(node),
                                                   MoveStructPat {
    override val referenceNameElement: PsiElement
        get() = qualifiedPath.identifier
}