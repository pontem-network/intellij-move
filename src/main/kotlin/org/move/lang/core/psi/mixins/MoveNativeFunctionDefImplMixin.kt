package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveNativeFunctionDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveNativeFunctionDefImplMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                               MoveNativeFunctionDef