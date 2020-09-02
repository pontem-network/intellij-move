package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.MoveFunctionSpec
import org.move.lang.core.psi.impl.MoveElementImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl

abstract class MoveFunctionSpecImplMixin(node: ASTNode) : MoveElementImpl(node),
                                                          MoveFunctionSpec {
    override val referenceNameElement: PsiElement
        get() = findNotNullChildByType(IDENTIFIER)

    override fun getReference(): MoveReference =
        MoveReferenceImpl(this)
}