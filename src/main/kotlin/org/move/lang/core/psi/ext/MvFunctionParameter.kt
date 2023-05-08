package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvFunctionParameter

// BindingPat has required name
val MvFunctionParameter.name: String get() = this.bindingPat.name

var MvFunctionParameter.resolveContext: MvFunction?
    get() = (this as MvFunctionParameterMixin).resolveContext
    set(value) {
        (this as MvFunctionParameterMixin).resolveContext = value
    }

abstract class MvFunctionParameterMixin(node: ASTNode) : MvElementImpl(node), MvFunctionParameter {
    var resolveContext: MvFunction? = null

    override fun getContext(): PsiElement? = resolveContext ?: super.getContext()
}
