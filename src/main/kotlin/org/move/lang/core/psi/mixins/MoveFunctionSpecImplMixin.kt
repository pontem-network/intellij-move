package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveFunctionSpec
import org.move.lang.core.psi.impl.MoveReferenceElementImpl

abstract class MoveFunctionSpecImplMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                          MoveFunctionSpec {
}