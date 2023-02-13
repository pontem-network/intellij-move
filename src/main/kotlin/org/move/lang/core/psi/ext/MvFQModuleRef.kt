package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvFQModuleRef
import org.move.lang.core.resolve.ref.MvFQModuleReference
import org.move.lang.core.resolve.ref.MvFQModuleReferenceImpl
import org.move.lang.moveProject

abstract class MvFQModuleRefMixin(node: ASTNode) : MvElementImpl(node),
                                                   MvFQModuleRef {
    override val identifier: PsiElement?
        get() = findChildByType(MvElementTypes.IDENTIFIER)

    override fun getReference(): MvFQModuleReference {
        return MvFQModuleReferenceImpl(this)
    }
}
