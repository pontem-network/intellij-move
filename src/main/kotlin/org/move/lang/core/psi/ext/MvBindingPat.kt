package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvBindingPat

abstract class MvBindingPatImplMixin(node: ASTNode): MvNamedElementImpl(node), MvBindingPat {

}