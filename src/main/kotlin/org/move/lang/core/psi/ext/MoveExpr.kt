package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap

abstract class MoveExprMixin(node: ASTNode) : MoveElementImpl(node), MoveExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? = null

}
