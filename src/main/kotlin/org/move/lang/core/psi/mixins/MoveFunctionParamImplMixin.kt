package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveFunctionParam
import org.move.lang.core.psi.impl.MoveNamedElementImpl

abstract class MoveFunctionParamImplMixin(node: ASTNode) : MoveNamedElementImpl(node),
                                                           MoveFunctionParam {}