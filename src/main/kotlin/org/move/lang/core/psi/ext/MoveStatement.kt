package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveStatement
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.VoidType

abstract class MoveStatementMixin(node: ASTNode): MoveElementImpl(node),
                                                  MoveStatement {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return VoidType()
    }
}
