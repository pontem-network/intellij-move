package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveNativeStructDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MoveNativeStructDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                         MoveNativeStructDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT
}