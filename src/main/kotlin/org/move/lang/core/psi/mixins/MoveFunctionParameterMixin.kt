package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.psi.ext.ty
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MvFunctionParameter.declaredTy: Ty get() = this.typeAnnotation?.type?.ty() ?: TyUnknown

abstract class MvFunctionParameterMixin(node: ASTNode) : MvElementImpl(node),
                                                         MvFunctionParameter {

    override fun getIcon(flags: Int): Icon = MvIcons.PARAMETER

//    override fun resolvedType(): Ty {
////        val type = this.typeAnnotation?.type ?: return TyUnknown
//        return this.typeAnnotation?.type?.inferTypeTy() ?: TyUnknown
//    }
}
