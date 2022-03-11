package org.move.lang.core.psi.mixins

import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MvSchemaMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                  MvSchema {
    override fun getIcon(flags: Int) = MvIcons.SCHEMA
}
