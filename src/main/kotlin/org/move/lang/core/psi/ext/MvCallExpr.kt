package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvValueArgumentList
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvTypeArgument
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferCallExprTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction

val MvCallExpr.typeArguments: List<MvTypeArgument> get() = this.path.typeArguments

val MvCallExpr.callArgumentExprs: List<MvExpr>
    get() = this.valueArgumentList
        ?.valueArgumentList.orEmpty().map { it.expr }

val MvValueArgumentList.argumentExprs: List<MvExpr> get() = this.valueArgumentList.map { it.expr }

fun MvCallExpr.acquiresTys(ctx: InferenceContext): List<Ty> =
    (inferCallExprTy(this, ctx, null) as? TyFunction)?.acquiresTypes.orEmpty()
