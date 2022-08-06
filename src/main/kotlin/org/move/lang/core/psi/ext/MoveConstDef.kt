package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvConst
import org.move.lang.core.types.infer.inferTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun MvConst.declaredTy(msl: Boolean): Ty =
    this.typeAnnotation?.type?.let { inferTypeTy(it, msl) } ?: TyUnknown
