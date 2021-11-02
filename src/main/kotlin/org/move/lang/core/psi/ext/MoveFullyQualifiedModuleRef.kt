package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveFQModuleRef
import org.move.lang.core.resolve.ref.MoveFQModuleReference
import org.move.lang.core.resolve.ref.MoveFQModuleReferenceImpl

abstract class MoveFQModuleRefMixin(node: ASTNode) : MoveElementImpl(node),
                                                     MoveFQModuleRef {
    override val identifier: PsiElement?
        get() = findChildByType(MoveElementTypes.IDENTIFIER)

    override fun getReference(): MoveFQModuleReference {
        return MoveFQModuleReferenceImpl(this)
    }
}
