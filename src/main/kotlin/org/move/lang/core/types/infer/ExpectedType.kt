package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.declaredTy
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.ty
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyUnknown

fun inferExpectedTy(element: MvElement, ctx: InferenceContext): Ty? {
    val owner = element.parent
    return when (owner) {
        is MvBorrowExpr -> {
            val refTy = inferExpectedTy(owner, ctx) as? TyReference ?: return null
            refTy.innerTy()
        }
        is MvValueArgument -> {
            val valueArgumentList = owner.parent as MvValueArgumentList
            val paramIndex =
                valueArgumentList.children.indexOfFirst { it.textRange.contains(owner.textOffset) }
            if (paramIndex == -1) return null

            val callExpr = valueArgumentList.parent as? MvCallExpr ?: return null
            val inferenceCtx = callExpr.functionInferenceCtx(callExpr.isMsl())
            inferenceCtx.callExprTypes[callExpr]
                ?.paramTypes
                ?.getOrNull(paramIndex)
        }

        is MvInitializer -> {
            val initializerParent = owner.parent
            when (initializerParent) {
                is MvLetStmt -> {
                    val pat = initializerParent.pat
                    when (pat) {
                        is MvBindingPat -> {
                            val ty = pat.declaredTy(ctx)
                            if (ty is TyUnknown) null else ty
                        }
                        is MvStructPat -> pat.ty()
                        else -> null
                    }
                }
                else -> null
            }
        }
        is MvStructLitField -> owner.ty()
        else -> null
    }
}
