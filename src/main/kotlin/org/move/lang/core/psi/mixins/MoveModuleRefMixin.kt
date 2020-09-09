package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveModuleRef
import org.move.lang.core.psi.impl.MoveReferenceElementImpl

abstract class MoveModuleRefMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                   MoveModuleRef {
}