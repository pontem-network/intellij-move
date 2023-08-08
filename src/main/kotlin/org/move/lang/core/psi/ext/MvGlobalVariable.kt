package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvGlobalVariableStmt
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MvGlobalVariableMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                                     MvGlobalVariableStmt {

    override fun getIcon(flags: Int): Icon? = MoveIcons.BINDING
                                                     }