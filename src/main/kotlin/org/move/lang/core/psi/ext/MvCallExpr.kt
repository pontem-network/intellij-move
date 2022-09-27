package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvTypeArgument
import org.move.lang.core.psi.MvValueArgument
import org.move.lang.core.types.infer.ownerInferenceCtx
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvCallExpr.typeArguments: List<MvTypeArgument> get() = this.path.typeArguments

val MvCallExpr.valueArguments: List<MvValueArgument>
    get() =
        this.valueArgumentList?.valueArgumentList.orEmpty()

val MvCallExpr.callArgumentExprs: List<MvExpr>
    get() = this.valueArgumentList
        ?.valueArgumentList.orEmpty().map { it.expr }

fun MvCallExpr.acquiresTys(): List<Ty>? {
    val msl = this.isMsl()
    val inferenceCtx = this.ownerInferenceCtx(msl)
    return inferenceCtx.callExprTypes[this]
        ?.acquiresTypes
        ?.takeIf { !it.contains(TyUnknown) }
}
