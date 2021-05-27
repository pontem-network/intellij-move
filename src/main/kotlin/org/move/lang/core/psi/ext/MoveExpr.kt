package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType

abstract class MoveExprMixin(node: ASTNode) : MoveElementImpl(node), MoveExpr {
    override fun resolvedType(): BaseType? = null

}
