package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

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

//fun MvCallExpr.inferAcquiresTys(): List<Ty>? {
//    val msl = this.isMsl()
////    val inferenceCtx = this.maybeInferenceContext(msl) ?: return null
//    val inference = this.inference(msl) ?: return null
//    return inference.getAcquiredTypes(this).takeIf { !it.contains(TyUnknown) }
////    return inferenceCtx.callExprTypes[this]
////        ?.acquiresTypes
////        ?.takeIf { !it.contains(TyUnknown) }
//}
