package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualTypeReferenceElementImpl
import org.move.lang.core.psi.MoveStructPat

abstract class MoveStructPatMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                   MoveStructPat {

}