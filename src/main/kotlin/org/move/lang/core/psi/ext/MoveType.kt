package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveType
import org.move.lang.core.types.infer.inferMoveTypeTy
import org.move.lang.core.types.ty.Ty

fun MoveType.inferTypeTy(): Ty = inferMoveTypeTy(this)
