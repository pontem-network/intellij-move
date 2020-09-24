package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*

abstract class MoveStructLiteralExprMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                           MoveStructLiteralExpr