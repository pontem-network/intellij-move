package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveNativeFunctionDef
import org.move.lang.core.psi.impl.MoveNamedElementImpl

abstract class MoveNativeFunctionDefImplMixin(node: ASTNode) : MoveNamedElementImpl(node), MoveNativeFunctionDef