package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveStructDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveStructDef {
}