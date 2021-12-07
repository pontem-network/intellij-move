package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvSchemaSpecDef
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvSchemaDefMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                   MvSchemaSpecDef {
}
