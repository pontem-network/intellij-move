package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualNameReferenceElementImpl
import org.move.lang.core.psi.MoveRefExpr

abstract class MoveRefExprMixin(node: ASTNode) : MoveQualNameReferenceElementImpl(node),
                                                 MoveRefExpr