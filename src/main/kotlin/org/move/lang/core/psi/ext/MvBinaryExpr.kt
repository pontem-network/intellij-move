package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes.BINARY_OP
import org.move.lang.core.MOVE_BINARY_OPS
import org.move.lang.core.psi.MvBinaryExpr
import org.move.lang.core.psi.MvElementImpl

abstract class MvBinaryExprMixin(node: ASTNode): MvElementImpl(node),
                                                 MvBinaryExpr {
    override fun toString(): String {
        val op = node.findChildByType(BINARY_OP)?.text ?: ""
        return "${javaClass.simpleName}(${node.elementType}[$op])"
    }
}
