package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualifiedPath
import org.move.lang.core.psi.impl.MoveReferenceElementImpl

abstract class MoveQualifiedPathMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                       MoveQualifiedPath {
}