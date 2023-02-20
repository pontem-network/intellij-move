package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.types.infer.ItemContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun MvFunctionParameter.paramTypeTy(itemContext: ItemContext): Ty =
    this.typeAnnotation
        ?.type
        ?.let { itemContext.getTypeTy(it) } ?: TyUnknown

val MvFunctionParameter.functionLike get() = this.parent.parent as? MvFunctionLike
