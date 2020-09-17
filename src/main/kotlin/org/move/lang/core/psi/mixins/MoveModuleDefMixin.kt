package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveModuleDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveModuleDef