package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveFunctionParameterList
import org.move.lang.core.psi.impl.MoveElementImpl

abstract class MoveFunctionParameterListMixin(node: ASTNode) : MoveElementImpl(node),
                                                               MoveFunctionParameterList {
}