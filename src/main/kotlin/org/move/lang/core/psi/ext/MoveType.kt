package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveType
import org.move.lang.core.types.BaseType

abstract class MoveTypeMixin(node: ASTNode) : MoveElementImpl(node), MoveType {
    override fun resolvedType(): BaseType? = null
}
