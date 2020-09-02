package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveFunctionParam
import org.move.lang.core.psi.impl.MoveNamedIdentifierOwnerImpl

abstract class MoveFunctionParamMixin(node: ASTNode) : MoveNamedIdentifierOwnerImpl(node),
                                                       MoveFunctionParam {}