package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyTypeParameter

abstract class MoveTypeParameterMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                       MoveTypeParameter {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return TyTypeParameter(this)
    }
}
