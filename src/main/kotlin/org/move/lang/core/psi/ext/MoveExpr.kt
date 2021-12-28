package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvExpr
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.ty.Ty

fun MvExpr.inferExprTy(ctx: InferenceContext = InferenceContext()) = inferExprTy(this, ctx)
