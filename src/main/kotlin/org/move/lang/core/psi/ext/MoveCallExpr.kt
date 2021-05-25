package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*


abstract class MoveCallExprMixin(node: ASTNode) : MoveQualNameReferenceElementImpl(node),
                                                  MoveCallExpr


fun MoveCallExpr.expectedParamsCount(): Int? {
    val referred = this.reference?.resolve()
//    val referred = this.qualPath.reference.resolve()
    if (referred is MoveFunctionSignatureOwner) {
        return referred.parameters.size
    }
    return null
}
