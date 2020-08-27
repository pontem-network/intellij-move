package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveNativeFunctionDef
import org.move.lang.core.psi.impl.MoveNamedElementImpl

abstract class MoveNativeFunctionDefImplMixin(node: ASTNode) : MoveNamedElementImpl(node), MoveNativeFunctionDef