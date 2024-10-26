package org.move.ide.inspections.compilerV2

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.compilerV2.fixes.ReplaceWithIndexExprFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.argumentExprs
import org.move.lang.core.psi.ext.is0x1Address
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.isCopy
import org.move.lang.moveProject

class MvReplaceWithIndexExprInspection:
    Move2OnlyInspectionBase<MvCallExpr>(MvCallExpr::class.java) {

    override fun visitTargetElement(element: MvCallExpr, holder: ProblemsHolder, isOnTheFly: Boolean) {
        val function = element.path.reference?.resolveFollowingAliases() as? MvFunction ?: return
        val module = function.module ?: return
        val moveProject = function.moveProject ?: return
        val msl = element.isMsl()
        val inference = element.inference(msl) ?: return
        val callExprRange = element.textRange
        // vector methods
        if (inference.typeErrors.any { callExprRange.contains(it.range()) }) {
            // type error inside the call expr
            return
        }
        if (module.name == "vector" && module.is0x1Address(moveProject)) {
            val parentExpr = element.parent
            if (function.name == "borrow" && parentExpr is MvDerefExpr) {
                val receiverParamExpr = element.argumentExprs.firstOrNull() ?: return
                if (receiverParamExpr is MvBorrowExpr) {
                    val itemExpr = receiverParamExpr.expr ?: return
                    if (!inference.getExprType(itemExpr).isCopy) {
                        // cannot borrow deref without copy
                        return
                    }
                }
                holder.registerProblem(
                    parentExpr,
                    "Can be replaced with index expr",
                    ProblemHighlightType.WEAK_WARNING,
                    ReplaceWithIndexExprFix(parentExpr)
                )
            }
        }
    }
}