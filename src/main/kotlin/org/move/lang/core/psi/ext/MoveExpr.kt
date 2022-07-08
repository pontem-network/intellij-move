package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyUnknown

fun MvExpr.inferExprTy(ctx: InferenceContext) = inferExprTy(this, ctx)

fun MvExpr.expectedTy(ctx: InferenceContext): Ty {
    val parent = this.findFirstParent { it is MvLetStmt || it is MvCallArgumentList } ?: return TyUnknown
    return when (parent) {
        is MvLetStmt -> {
            val pat = parent.pat
            return when (pat) {
                is MvBindingPat -> pat.declaredTy(ctx)
                else -> TyUnknown
            }
        }
        is MvCallArgumentList -> {
            for ((i, childExpr) in parent.children.withIndex()) {
                if (childExpr.textRange.contains(this.textOffset)) {
                    val callExpr = parent.parent as? MvCallExpr ?: return TyUnknown
                    val callTy = callExpr.inferCallTy(ctx) as? TyFunction ?: return TyUnknown
                    return callTy.paramTypes[i]
                }
            }
            TyUnknown
        }
        else -> TyUnknown
    }
}
