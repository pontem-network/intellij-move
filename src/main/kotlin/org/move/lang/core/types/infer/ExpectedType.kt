package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.structLitExpr
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct

fun inferExpectedTy(element: PsiElement, parentCtx: InferenceContext): Ty? {
    val owner = element.parent
    return when (owner) {
        is MvBorrowExpr -> {
            val refTy = inferExpectedTy(owner, parentCtx) as? TyReference ?: return null
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
                    val inferenceCtx = ownerExpr.maybeInferenceContext(ownerExpr.isMsl()) ?: return null
                    inferenceCtx.callExprTypes[ownerExpr]
                        ?.typeVars
                        ?.getOrNull(paramIndex)
                }
                is MvStructLitExpr -> {
                    val inferenceCtx = ownerExpr.maybeInferenceContext(ownerExpr.isMsl()) ?: return null
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
            val inferenceCtx = callExpr.maybeInferenceContext(callExpr.isMsl()) ?: return null
            inferenceCtx.callExprTypes[callExpr]
                ?.paramTypes
                ?.getOrNull(paramIndex)
        }

        is MvInitializer -> {
            val initializerParent = owner.parent
            when (initializerParent) {
                is MvLetStmt -> {
                    val explicitTy =
                        initializerParent.typeAnnotation?.type?.let { parentCtx.getTypeTy(it) }
                    initializerParent.pat
                        ?.let { inferPatTy(it, parentCtx, explicitTy) }
                }
                else -> null
            }
        }
        is MvStructLitField -> {
            // only first level field for now, rewrite later as recursive
            val structLitExpr = owner.structLitExpr
            val structExpectedTy = inferExpectedTy(structLitExpr, parentCtx)
            val structTy = inferExprTy(structLitExpr, parentCtx, structExpectedTy) as? TyStruct ?: return null
            structTy.fieldTy(owner.referenceName)
        }
        else -> null
    }
}
