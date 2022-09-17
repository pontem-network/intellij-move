package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.structLitExpr
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct

fun inferExpectedTy(element: PsiElement, ctx: InferenceContext): Ty? {
    val owner = element.parent
    return when (owner) {
        is MvBorrowExpr -> {
            val refTy = inferExpectedTy(owner, ctx) as? TyReference ?: return null
            refTy.innerTy()
        }
        is MvTypeArgument -> {
            val typeArgumentList = owner.parent as MvTypeArgumentList
            val paramIndex =
                typeArgumentList.children.indexOfFirst { it.textRange.contains(owner.textOffset) }
            if (paramIndex == -1) return null
            val path = typeArgumentList.parent as MvPath
            val ownerExpr = path.parent
            when (ownerExpr) {
                is MvCallExpr -> {
                    val inferenceCtx = ownerExpr.ownerInferenceCtx(ownerExpr.isMsl())
                    inferenceCtx.callExprTypes[ownerExpr]
                        ?.typeVars
                        ?.getOrNull(paramIndex)
                }
                is MvStructLitExpr -> {
                    val inferenceCtx = ownerExpr.ownerInferenceCtx(ownerExpr.isMsl())
                    (inferenceCtx.exprTypes[ownerExpr] as? TyStruct)
                        ?.typeArgs
                        ?.getOrNull(paramIndex)
                }
                else -> null
            }
        }
        is MvValueArgument -> {
            val valueArgumentList = owner.parent as MvValueArgumentList
            val paramIndex =
                valueArgumentList.children.indexOfFirst { it.textRange.contains(owner.textOffset) }
            if (paramIndex == -1) return null
            val callExpr = valueArgumentList.parent as? MvCallExpr ?: return null
            val inferenceCtx = callExpr.ownerInferenceCtx(callExpr.isMsl())
            inferenceCtx.callExprTypes[callExpr]
                ?.paramTypes
                ?.getOrNull(paramIndex)
        }

        is MvInitializer -> {
            val initializerParent = owner.parent
            when (initializerParent) {
                is MvLetStmt -> {
                    val patExplicitTy = initializerParent.typeAnnotation?.type?.let { inferTypeTy(it, ctx.msl) }
                    initializerParent.pat
                        ?.let { inferPatTy(it, ctx, patExplicitTy) }
                }
                else -> null
            }
        }
        is MvStructLitField -> {
            // only first level field for now, rewrite later as recursive
            val structLitExpr = owner.structLitExpr
            val structExpectedTy = inferExpectedTy(structLitExpr, ctx)
            val structTy = inferExprTy(structLitExpr, ctx, structExpectedTy) as? TyStruct ?: return null
            structTy.fieldTy(owner.referenceName)
        }
        else -> null
    }
}
