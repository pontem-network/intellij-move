package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MoveStructDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveStructDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT
}