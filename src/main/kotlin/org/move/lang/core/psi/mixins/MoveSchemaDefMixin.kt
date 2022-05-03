package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvSchemaMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                  MvSchema {
    override fun getIcon(flags: Int) = MoveIcons.SCHEMA
}
