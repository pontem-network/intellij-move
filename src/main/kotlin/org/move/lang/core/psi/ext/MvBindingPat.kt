package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.MvIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.inferBindingTy
import org.move.lang.core.types.ty.Ty
import javax.swing.Icon

val MvBindingPat.owner: PsiElement?
    get() = PsiTreeUtil.findFirstParent(this) {
        it is MvLetStatement || it is MvFunctionParameter || it is MvConstDef
    }

fun MvBindingPat.inferBindingPatTy(): Ty = inferBindingTy(this)

abstract class MvBindingPatMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                  MvBindingPat {

    override fun getIcon(flags: Int): Icon =
        when (this.owner) {
            is MvFunctionParameter -> MvIcons.PARAMETER
            is MvConstDef -> MvIcons.CONST
            else -> MvIcons.VARIABLE
        }
}
