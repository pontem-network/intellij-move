package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.ty.Ty

fun MoveExpr.inferExprTy(ctx: InferenceContext = InferenceContext()): Ty =
    inferExprTy(this, ctx)
