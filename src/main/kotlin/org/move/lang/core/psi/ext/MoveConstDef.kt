package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvConstDef
import org.move.lang.core.types.infer.inferMvTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun MvConstDef.declaredTy(msl: Boolean): Ty =
    this.typeAnnotation?.type?.let { inferMvTypeTy(it, msl) } ?: TyUnknown
