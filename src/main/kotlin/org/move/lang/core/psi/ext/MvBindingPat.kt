package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvMandatoryNameIdentifierOwnerImpl
import javax.swing.Icon

val MvBindingPat.owner: PsiElement?
    get() = PsiTreeUtil.findFirstParent(this) {
        it is MvLetStmt
                || it is MvFunctionParameter
                || it is MvSchemaFieldStmt
    }

var MvBindingPat.synthetic: Boolean
    get() = (this as MvBindingPatMixin).synthetic
    set(value) {
        (this as MvBindingPatMixin).synthetic = value
    }

abstract class MvBindingPatMixin(node: ASTNode) : MvMandatoryNameIdentifierOwnerImpl(node),
                                                  MvBindingPat {
    var synthetic = false

    override fun getIcon(flags: Int): Icon =
        when (this.owner) {
            is MvFunctionParameter -> MoveIcons.PARAMETER
            is MvConst -> MoveIcons.CONST
            else -> MoveIcons.VARIABLE
        }

    override fun getUseScope(): SearchScope {
        return when (this.owner) {
            is MvFunctionParameter -> {
                val function = this.ancestorStrict<MvFunction>() ?: return super.getUseScope()
                var combinedScope: SearchScope = LocalSearchScope(function)
                for (itemSpec in function.innerItemSpecs()) {
                    combinedScope = combinedScope.union(LocalSearchScope(itemSpec))
                }
                for (itemSpec in function.outerItemSpecs()) {
                    combinedScope = combinedScope.union(LocalSearchScope(itemSpec))
                }
                combinedScope
            }
            is MvLetStmt -> {
                val function = this.ancestorStrict<MvFunction>() ?: return super.getUseScope()
                LocalSearchScope(function)
            }
            is MvSchemaFieldStmt -> super.getUseScope()
            is MvConst -> super.getUseScope()
            else -> super.getUseScope()
        }
    }
}
