package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvModuleDef

abstract class MvModuleDefImplMixin(node: ASTNode) : MvNamedElementImpl(node), MvModuleDef