package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveDefineFunction
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MoveDefineFunctionMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                        MoveDefineFunction {

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION
}