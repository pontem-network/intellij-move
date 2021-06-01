package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveFunctionParameter
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.BaseType
import javax.swing.Icon

abstract class MoveFunctionParameterMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                           MoveFunctionParameter {

    override fun getIcon(flags: Int): Icon = MoveIcons.PARAMETER

    override fun resolvedType(): BaseType? {
        return this.typeAnnotation?.type?.resolvedType()
    }
}
