package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveConstDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveConstDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                  MoveConstDef {
}