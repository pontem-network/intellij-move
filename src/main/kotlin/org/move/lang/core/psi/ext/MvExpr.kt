package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvExpr
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.infer.maybeInferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun MvExpr.inferredExprTy(): Ty {
    val msl = this.isMsl()
    val inferenceCtx = this.maybeInferenceContext(msl) ?: return TyUnknown

//    val existingTy = inferenceCtx.exprTypes[this]
//    if (existingTy != null) {
//        return existingTy
//    }
    return inferExprTy(this, inferenceCtx)
}
