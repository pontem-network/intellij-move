package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.maybeInferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvCallExpr.typeArguments: List<MvTypeArgument> get() = this.path.typeArguments

val MvCallExpr.valueArguments: List<MvValueArgument>
    get() =
        this.valueArgumentList?.valueArgumentList.orEmpty()

val MvCallExpr.callArgumentExprs: List<MvExpr>
    get() = this.valueArgumentList
        ?.valueArgumentList.orEmpty().map { it.expr }

val MvMacroCallExpr.callArgumentExprs: List<MvExpr>
    get() = this.valueArgumentList
        ?.valueArgumentList.orEmpty().map { it.expr }

fun MvCallExpr.inferAcquiresTys(): List<Ty>? {
    val msl = this.isMsl()
//    val inferenceCtx = this.maybeInferenceContext(msl) ?: return null
    val inference = this.inference(msl) ?: return null
    return inference.getAcquiredTypes(this).takeIf { !it.contains(TyUnknown) }
//    return inferenceCtx.callExprTypes[this]
//        ?.acquiresTypes
//        ?.takeIf { !it.contains(TyUnknown) }
}
