package org.move.ide.inspections.compilerV2

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.compilerV2.fixes.ReplaceWithMethodCallFix
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.itemModule
import org.move.lang.core.psi.ext.valueArguments
import org.move.lang.core.psi.module
import org.move.lang.core.psi.selfParamTy
import org.move.lang.core.types.infer.deepFoldTyTypeParameterWith
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyReference.Companion.isCompatibleWithAutoborrow
import org.move.lang.core.types.ty.hasTyUnknown
import org.move.lang.moveProject

class MvReplaceWithMethodCallInspection:
    Move2OnlyInspectionBase<MvCallExpr>(MvCallExpr::class.java) {

    override fun visitTargetElement(element: MvCallExpr, holder: ProblemsHolder, isOnTheFly: Boolean) {
        val function = element.path.reference?.resolveFollowingAliases() as? MvFunction ?: return
        val msl = element.isMsl()
        val inference = element.inference(msl) ?: return

        val firstArgExpr = element.valueArguments.firstOrNull()?.expr ?: return
        val firstArgExprTy = inference.getExprType(firstArgExpr)
        if (firstArgExprTy.hasTyUnknown) return

        val moveProject = element.moveProject ?: return
        val methodModule = function.module ?: return
        val itemModule = firstArgExprTy.itemModule(moveProject) ?: return
        if (methodModule != itemModule) return

        val selfTy = function.selfParamTy(msl)
            ?.deepFoldTyTypeParameterWith { TyInfer.TyVar(it) } ?: return
        if (selfTy.hasTyUnknown) return

        if (isCompatibleWithAutoborrow(firstArgExprTy, selfTy, msl)) {
            // can be converted
            holder.registerProblem(
                element,
                "Can be replaced with method call",
                ProblemHighlightType.WEAK_WARNING,
                ReplaceWithMethodCallFix(element)
            )
        }
    }
}