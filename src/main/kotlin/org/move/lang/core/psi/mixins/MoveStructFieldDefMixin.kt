package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructFieldDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveStructFieldDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                        MoveStructFieldDef {
}