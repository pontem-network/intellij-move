package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.structLitExpr
import org.move.lang.core.types.ty.*

fun inferExpectedTy(element: PsiElement, inference: InferenceResult): Ty? {
    val owner = element.parent
    return when (owner) {
        is MvTypeArgument -> {
            val typeArgumentList = owner.parent as MvTypeArgumentList
            val paramIndex =
                typeArgumentList.children.indexOfFirst { it.textRange.contains(owner.textOffset) }
            if (paramIndex == -1) return null

            val path = typeArgumentList.parent as MvPath
            val genericItem = path.reference?.resolveWithAliases() as? MvTypeParametersOwner ?: return null
            genericItem.typeParameters.getOrNull(paramIndex)?.let { TyTypeParameter(it) }
//            val ownerExpr = path.parent as? MvExpr ?: return null

//            val msl = ownerExpr.isMsl()
//            val inference = ownerExpr.inference(msl) ?: return null

//            when (ownerExpr) {
//                is MvCallExpr,
//                is MvStructLitExpr -> {
//                    val function = ownerExpr.path.reference?.resolveWithAliases() as? MvTypeParametersOwner ?: return null
//                    function.type
//                    val funcTy = inference.getExprType(ownerExpr) as? TyFunction ?: return null
//                    funcTy.typeVars.getOrNull(paramIndex)
////                    val inferenceCtx = ownerExpr.maybeInferenceContext(msl) ?: return null
////                    inferenceCtx.callExprTypes[ownerExpr]
////                        ?.typeVars
////                        ?.getOrNull(paramIndex)
//                }
//                is MvStructLitExpr -> {
//                    val structTy = inference.getExprType(ownerExpr) as? TyStruct ?: return null
////                    val inferenceCtx = ownerExpr.maybeInferenceContext(msl) ?: return null
//                    structTy.typeArgs.getOrNull(paramIndex)
////                    (inferenceCtx.exprTypes[ownerExpr] as? TyStruct)
////                        ?.typeArgs
////                        ?.getOrNull(paramIndex)
////                    (inferenceCtx.exprTypes[ownerExpr] as? TyStruct)
////                        ?.typeArgs
////                        ?.getOrNull(paramIndex)
//                }
//                else -> null
//            }
        }
//                is MvValueArgument -> {
//                    val expr = element as? MvExpr ?: return null
//                    inference.getExpectedType(expr)
////            val valueArgumentList = owner.parent as MvValueArgumentList
////            val paramIndex =
////                valueArgumentList.children.indexOfFirst { it.textRange.contains(owner.textOffset) }
////            if (paramIndex == -1) return null
////            val callExpr = valueArgumentList.parent as? MvCallExpr ?: return null
////
//////            val msl = callExpr.isMsl()
//////            val inference = callExpr.inference(msl) ?: return null
////            val callTy = inference.getExprType(callExpr) as? TyFunction ?: return null
////            callTy.paramTypes.getOrNull(paramIndex)
//////            val inferenceCtx = callExpr.maybeInferenceContext(callExpr.isMsl()) ?: return null
//////            inferenceCtx.callExprTypes[callExpr]
//////                ?.paramTypes
//////                ?.getOrNull(paramIndex)
//                }

//        is MvInitializer -> {
//            val initializerParent = owner.parent
//            when (initializerParent) {
//                is MvLetStmt -> {
////                    val explicitTy =
////                        initializerParent.typeAnnotation?.type?.let { parentCtx.getTypeTy(it) }
//                    initializerParent.pat?.let { inference.getPatType(it) }
////                        ?.let { inferPatTy(it, parentCtx, explicitTy) }
//                }
//                else -> null
//            }
//        }
//        is MvStructLitField -> {
//            // only first level field for now, rewrite later as recursive
//            val structLitExpr = owner.structLitExpr
////            val structExpectedTy = inferExpectedTy(structLitExpr, inference)
//            val structTy = inference.getExprType(structLitExpr) as? TyStruct ?: return null
////            val structTy = inferExprTy(structLitExpr, parentCtx, structExpectedTy) as? TyStruct ?: return null
//            structTy.fieldTy(owner.referenceName)
//        }
        else -> if (element is MvExpr) inference.getExpectedType(element) else null
    }
//    return when (element) {
//        is MvExpr -> inference.getExpectedType(element)
//        else -> {
//
//        }
//
//    }
}
