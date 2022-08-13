package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.declaredTy
import org.move.lang.core.psi.ext.ty
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyUnknown

fun inferExprExpectedTy(expr: MvExpr, ctx: InferenceContext): Ty {
    val owner = expr.parent
    return when (owner) {
        is MvCallArgumentList -> {
            val paramIndex =
                owner.children.indexOfFirst { it.textRange.contains(expr.textOffset) }
            if (paramIndex == -1) return TyUnknown

            val callExpr = owner.parent as? MvCallExpr ?: return TyUnknown
            val callTy = inferCallExprTy(callExpr, ctx, null) as? TyFunction ?: return TyUnknown
            callTy.paramTypes[paramIndex]
        }

        is MvInitializer -> {
            val initializerParent = owner.parent
            when (initializerParent) {
                is MvLetStmt -> {
                    val pat = initializerParent.pat
                    when (pat) {
                        is MvBindingPat -> pat.declaredTy(ctx)
                        is MvStructPat -> pat.ty()
                        else -> TyUnknown
                    }
                }

                else -> TyUnknown
            }
        }
        is MvStructLitField -> owner.ty()
        else -> TyUnknown
    }
}
