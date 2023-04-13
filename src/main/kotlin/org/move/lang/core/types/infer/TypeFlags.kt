package org.move.lang.core.types.infer

import org.move.lang.core.types.ty.Ty

typealias TypeFlags = Int

const val HAS_TY_INFER_MASK: TypeFlags = 1
const val HAS_TY_TYPE_PARAMETER_MASK: TypeFlags = 2

fun mergeFlags(tys: Collection<Ty>): TypeFlags =
    tys.fold(0) { a, b -> a or b.flags }
