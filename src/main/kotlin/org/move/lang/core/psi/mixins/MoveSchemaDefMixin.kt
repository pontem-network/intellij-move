package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvSchemaMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                  MvSchema {
}
