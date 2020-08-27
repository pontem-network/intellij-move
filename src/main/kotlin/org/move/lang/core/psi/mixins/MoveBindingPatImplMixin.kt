package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveBindingPatImplMixin(node: ASTNode): MoveNameIdentifierOwnerImpl(node), MoveBindingPat {

}