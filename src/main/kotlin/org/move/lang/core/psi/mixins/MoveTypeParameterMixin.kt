package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeParamType
import org.move.lang.core.types.TypeVarsMap

abstract class MoveTypeParameterMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                       MoveTypeParameter {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return TypeParamType(this)
    }
}
