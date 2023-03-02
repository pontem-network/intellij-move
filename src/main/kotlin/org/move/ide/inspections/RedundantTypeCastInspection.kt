package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.move.ide.inspections.fixes.RemoveRedundantCastFix
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.endOffsetInParent
import org.move.lang.core.psi.ext.inferredExprTy
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inferenceContext
import org.move.lang.core.types.ty.TyInteger
import org.move.lang.core.types.ty.TyUnknown

class RedundantTypeCastInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitCastExpr(castExpr: MvCastExpr) {
            val exprTy = castExpr.expr.inferredExprTy()
            if (exprTy is TyUnknown) return

            // cannot be redundant cast for untyped integer
            if (exprTy is TyInteger && (exprTy.kind == TyInteger.DEFAULT_KIND)) return

            val msl = castExpr.isMsl()
            // TODO: different rules for msl, no need for any casts at all
            if (msl) return

//            val itemContext = castExpr.itemContextOwner?.itemContext(false) ?: castExpr.project.itemContext(false)
            val inferenceCtx = castExpr.inferenceContext(false)
            val castTy = inferenceCtx.getTypeTy(castExpr.type)
            if (castTy is TyUnknown) return

            if (exprTy == castTy) {
                holder.registerProblem(
                    castExpr,
                    "No cast needed",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    TextRange.create(castExpr.`as`.startOffsetInParent, castExpr.type.endOffsetInParent),
                    RemoveRedundantCastFix(castExpr)
                )
            }
        }
    }
}
