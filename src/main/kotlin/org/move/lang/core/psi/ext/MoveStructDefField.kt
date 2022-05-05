package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvStructField
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MvStructFieldMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                   MvStructField {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT_FIELD
}
