package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvType
import org.move.lang.core.types.infer.inferMvTypeTy
import org.move.lang.core.types.ty.Ty

fun MvType.inferTypeTy(): Ty = inferMvTypeTy(this)
