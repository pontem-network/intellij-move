package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveCallExpr
import org.move.lang.core.psi.MoveFunctionSignatureOwner
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.psi.parameters


abstract class MoveCallExprMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                  MoveCallExpr {
    override val referenceNameElement: PsiElement
        get() = qualifiedPath.identifier
}


fun MoveCallExpr.expectedParamsCount(): Int? {
    val referred = this.qualifiedPath.reference.resolve()
    if (referred is MoveFunctionSignatureOwner) {
        return referred.parameters.size
    }
    return null
}
