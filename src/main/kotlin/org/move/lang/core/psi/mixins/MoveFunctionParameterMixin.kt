package org.move.lang.core.psi.mixins

import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.types.infer.inferTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun MvFunctionParameter.declaredTy(msl: Boolean): Ty =
    this.typeAnnotation?.type?.let { inferTypeTy(it, msl) } ?: TyUnknown
