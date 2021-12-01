package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveFunctionParameter
import org.move.lang.core.psi.ext.inferTypeTy
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MoveFunctionParameter.declaredTy: Ty get() = this.typeAnnotation?.type?.inferTypeTy() ?: TyUnknown

abstract class MoveFunctionParameterMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                           MoveFunctionParameter {

    override fun getIcon(flags: Int): Icon = MoveIcons.PARAMETER

    override fun resolvedType(): Ty {
//        val type = this.typeAnnotation?.type ?: return TyUnknown
        return this.typeAnnotation?.type?.inferTypeTy() ?: TyUnknown
    }
}
