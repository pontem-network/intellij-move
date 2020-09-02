package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveLetExpr
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.ext.boundElements
import org.move.lang.core.psi.impl.MoveElementImpl

abstract class MoveLetExprMixin(node: ASTNode) : MoveElementImpl(node),
                                                 MoveLetExpr {
}