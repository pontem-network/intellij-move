package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExprExpectedTy
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.infer.inferenceCtx
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyUnknown

fun MvExpr.inferredTy(): Ty {
    val msl = this.isMsl()
    val inferenceCtx = this.containingFunction?.inferenceCtx(msl) ?: InferenceContext(msl)
    return inferExprTy(this, inferenceCtx)
}

fun MvExpr.inferExprTy(ctx: InferenceContext) = inferExprTy(this, ctx)

fun MvExpr.expectedTy(ctx: InferenceContext): Ty = inferExprExpectedTy(this, ctx)
