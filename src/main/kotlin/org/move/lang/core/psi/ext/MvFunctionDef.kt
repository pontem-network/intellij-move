package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvFunctionDef

abstract class MvFunctionDefImplMixin(node: ASTNode): MvNamedElementImpl(node), MvFunctionDef