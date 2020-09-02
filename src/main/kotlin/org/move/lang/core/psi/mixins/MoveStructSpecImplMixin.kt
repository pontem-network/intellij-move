package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructSpec
import org.move.lang.core.psi.impl.MoveTypeReferenceElementImpl

abstract class MoveStructSpecImplMixin(node: ASTNode) : MoveTypeReferenceElementImpl(node),
                                               MoveStructSpec {
}