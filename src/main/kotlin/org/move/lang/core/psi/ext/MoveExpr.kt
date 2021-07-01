package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MoveElementTypes.R_BRACE
import org.move.lang.core.psi.MoveCodeBlock
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap

fun MoveExpr.isBlockReturnExpr(): Boolean {
    val parentIsCodeBlock = this.parent is MoveCodeBlock
    val rightSiblingIsRBrace = this.getNextNonCommentSibling()?.elementType == R_BRACE
    return parentIsCodeBlock && rightSiblingIsRBrace
}

abstract class MoveExprMixin(node: ASTNode) : MoveElementImpl(node), MoveExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? = null

}
