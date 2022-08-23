package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.MODULE_KW
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvItemSpecRefReferenceImpl
import org.move.lang.core.resolve.ref.MvReference

val MvItemSpec.item: MvNamedElement? get() = this.itemSpecRef?.reference?.resolve()

val MvItemSpec.funcItem get() = this.item as? MvFunction
val MvItemSpec.structItem get() = this.item as? MvStruct

val MvItemSpec.itemSpecBlock: MvItemSpecBlock? get() = this.childOfType()

val MvItemSpecRef.moduleKw: PsiElement? get() = this.findFirstChildByType(MODULE_KW)

abstract class MvItemSpecRefMixin(node: ASTNode) : MvElementImpl(node), MvItemSpecRef {
    override fun getReference(): MvReference? {
        return if (this.moduleKw != null) null
        else
            MvItemSpecRefReferenceImpl(this)
    }
}
