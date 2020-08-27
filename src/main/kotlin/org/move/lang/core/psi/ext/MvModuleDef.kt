package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.impl.MoveNamedElementImpl

abstract class MoveModuleDefImplMixin(node: ASTNode) : MoveNamedElementImpl(node), MoveModuleDef