package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.impl.MvNamedElementImpl
import javax.swing.Icon

val MvConst.module: MvModule? get() = this.parent as? MvModule

abstract class MvConstMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                            MvConst {

    override fun getIcon(flags: Int): Icon? = MoveIcons.CONST
}
