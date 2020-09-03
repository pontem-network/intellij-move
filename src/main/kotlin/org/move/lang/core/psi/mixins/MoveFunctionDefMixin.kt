package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MoveFunctionDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                     MoveFunctionDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION
}