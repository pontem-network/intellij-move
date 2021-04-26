package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveStructSignature
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MoveStructSignatureMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                         MoveStructSignature {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT
}
