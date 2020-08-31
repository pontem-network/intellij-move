package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveFunctionDefImplMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                         MoveFunctionDef {
}