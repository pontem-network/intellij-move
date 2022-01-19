package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvSpecSchema
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvSpecSchemaMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                  MvSpecSchema {
}
