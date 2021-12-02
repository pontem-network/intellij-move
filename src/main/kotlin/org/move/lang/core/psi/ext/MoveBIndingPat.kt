package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveConstDef
import org.move.lang.core.psi.MoveFunctionParameter
import org.move.lang.core.psi.MoveLetStatement
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.inferBindingTy
import org.move.lang.core.types.ty.Ty
import javax.swing.Icon

val MoveBindingPat.owner: PsiElement?
    get() = PsiTreeUtil.findFirstParent(this) {
        it is MoveLetStatement || it is MoveFunctionParameter || it is MoveConstDef
    }

fun MoveBindingPat.inferBindingPatTy(): Ty = inferBindingTy(this)

abstract class MoveBindingPatMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                    MoveBindingPat {

    override fun getIcon(flags: Int): Icon =
        when (this.owner) {
            is MoveFunctionParameter -> MoveIcons.PARAMETER
            is MoveConstDef -> MoveIcons.CONST
            else -> MoveIcons.VARIABLE
        }
}
