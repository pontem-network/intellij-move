package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveCallExpr
import org.move.lang.core.psi.MoveFunctionSignature
import org.move.lang.core.psi.MoveQualNameReferenceElementImpl
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.VoidType


abstract class MoveCallExprMixin(node: ASTNode) : MoveQualNameReferenceElementImpl(node),
                                                  MoveCallExpr {
    override fun resolvedType(): BaseType? {
        val signature = this.reference.resolve() as? MoveFunctionSignature ?: return null
        val returnTypeElement = signature.returnType
        if (returnTypeElement == null) {
            return VoidType()
        }
        return returnTypeElement.type?.resolvedType()
    }
}
