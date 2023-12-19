package org.move.lang.core.types.infer

import org.move.lang.core.psi.MvDotExpr
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown

fun MvDotExpr.inferReceiverTy(msl: Boolean): Ty {
    val receiverExpr = this.expr
    val inference = receiverExpr.inference(msl) ?: return TyUnknown
    val receiverTy =
        inference.getExprTypeOrNull(receiverExpr) ?: run {
            // Sometimes in the completion, tree drastically changes between
            // `s.` and `s.IntellijIdeaRulezzz`, in that case inference cache value is inapplicable.
            // We don't want to drop the cache in that case, so to mitigate stale cache problems
            // - we just create another inference context (but only in case of an error).
            // Should happen pretty rarely, so hopefully won't affect performance much.
            val inferenceOwner =
                receiverExpr.ancestorOrSelf<MvInferenceContextOwner>() ?: return TyUnknown
            val noCacheInference = inferTypesIn(inferenceOwner, msl)
            noCacheInference.getExprType(receiverExpr)
        }

    val innerTy = when (receiverTy) {
        is TyReference -> receiverTy.innerTy() as? TyStruct ?: TyUnknown
        is TyStruct -> receiverTy
        else -> TyUnknown
    }
    return innerTy
}
