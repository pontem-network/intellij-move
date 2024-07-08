package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.psi.MvFunctionParameterList

// BindingPat has required name
val MvFunctionParameter.name: String get() = this.bindingPat.name

val MvFunctionParameter.paramIndex: Int get() =
    (this.parent as MvFunctionParameterList).functionParameterList.indexOf(this)

val MvFunctionParameter.isSelfParam: Boolean get() =
    this.bindingPat.name == "self" && this.paramIndex == 0

var MvFunctionParameter.resolveContext: MvFunction?
    get() = (this as MvFunctionParameterMixin).resolveContext
    set(value) {
        (this as MvFunctionParameterMixin).resolveContext = value
    }

abstract class MvFunctionParameterMixin(node: ASTNode) : MvElementImpl(node), MvFunctionParameter {
    var resolveContext: MvFunction? = null

    override fun getContext(): PsiElement? = resolveContext ?: super.getContext()
}
