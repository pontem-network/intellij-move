package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveType
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyTypeParameter

fun MoveTypeParameter.ty(): TyTypeParameter = TyTypeParameter(this)

abstract class MoveTypeParameterMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                       MoveTypeParameter {
//    override fun resolvedType(): Ty {
//        return TyTypeParameter(this)
//    }
}
