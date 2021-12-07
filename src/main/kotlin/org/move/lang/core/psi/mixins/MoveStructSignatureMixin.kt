package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvStructSignature
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MvStructSignatureMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                         MvStructSignature {

    override fun getIcon(flags: Int): Icon = MvIcons.STRUCT
}
