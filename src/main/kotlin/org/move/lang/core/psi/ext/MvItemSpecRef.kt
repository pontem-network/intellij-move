package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.resolve.ref.MvItemSpecRefReferenceImpl
import org.move.lang.core.resolve.ref.MvReference

val MvItemSpecRef.moduleKw: PsiElement? get() = this.findFirstChildByType(MvElementTypes.MODULE_KW)

abstract class MvItemSpecRefMixin(node: ASTNode) : MvElementImpl(node), MvItemSpecRef {
    override fun getReference(): MvReference? {
        return if (this.moduleKw != null) null
        else
            MvItemSpecRefReferenceImpl(this)
    }
}
