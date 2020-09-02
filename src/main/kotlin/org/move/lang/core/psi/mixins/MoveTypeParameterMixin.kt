package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveTypeParameterMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                       MoveTypeParameter {
}