package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveSchemaSpecDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveSchemaDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveSchemaSpecDef {
}
