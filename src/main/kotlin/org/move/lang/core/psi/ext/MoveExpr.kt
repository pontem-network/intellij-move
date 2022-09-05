package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvExpr
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.functionInferenceCtx
import org.move.lang.core.types.infer.inferExprExpectedTy
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.ty.Ty

fun MvExpr.inferredTy(): Ty {
    val msl = this.isMsl()
    val inferenceCtx = this.functionInferenceCtx(msl)

    val existingTy = inferenceCtx.exprTypes[this]
    if (existingTy != null) {
        return existingTy
    }
    return inferExprTy(this, inferenceCtx)
}

fun MvExpr.expectedTy(ctx: InferenceContext): Ty? = inferExprExpectedTy(this, ctx)
