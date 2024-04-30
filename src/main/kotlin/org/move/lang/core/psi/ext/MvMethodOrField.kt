package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvDotExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.resolve.ref.MvMandatoryReferenceElement
import org.move.lang.core.types.infer.MvInferenceContextOwner
import org.move.lang.core.types.infer.inferTypesIn
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

interface MvMethodOrField: MvMandatoryReferenceElement

val MvMethodOrField.dotExpr: MvDotExpr get() = parent as MvDotExpr
val MvMethodOrField.receiverExpr: MvExpr get() = dotExpr.expr

fun MvMethodOrField.inferReceiverTy(msl: Boolean): Ty {
    val receiverExpr = this.receiverExpr
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
    return receiverTy
}

