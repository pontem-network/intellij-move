package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvNativeFunctionDef

abstract class MvNativeFunctionDefImplMixin(node: ASTNode) : MvNamedElementImpl(node), MvNativeFunctionDef