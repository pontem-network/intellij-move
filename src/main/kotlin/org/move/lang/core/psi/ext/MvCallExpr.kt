package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvTypeArgument
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferCallExprTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction

val MvCallExpr.typeArguments: List<MvTypeArgument> get() = this.path.typeArguments

val MvCallExpr.arguments: List<MvExpr> get() = this.callArgumentList?.exprList.orEmpty()

fun MvCallExpr.acquiresTys(ctx: InferenceContext): List<Ty> =
    (inferCallExprTy(this, ctx, null) as? TyFunction)?.acquiresTypes.orEmpty()
