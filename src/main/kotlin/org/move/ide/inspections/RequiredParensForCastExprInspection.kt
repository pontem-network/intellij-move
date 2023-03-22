package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.descendantsOfType
import org.move.ide.inspections.fixes.WrapWithParensExprFix
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvParensExpr
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.endOffsetInParent
import org.move.lang.core.psi.ext.isAtomExpr
import org.move.lang.core.psi.ext.smartPointer

class RequiredParensForCastExprInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitCastExpr(castExpr: MvCastExpr) {
                val parent = castExpr.parent
                if (parent !is MvParensExpr) {
                    val fixes = mutableListOf(
                        WrapWithParensExprFix(castExpr, null)
                    )
                    val childAtomExpr = castExpr.expr
                        .descendantsOfType<MvExpr>().lastOrNull()
                        ?.takeIf { it.isAtomExpr }
                    if (childAtomExpr != null) {
                        fixes.add(0, WrapWithParensExprFix(castExpr, childAtomExpr.smartPointer()))
                    }
                    holder.registerProblem(
                        castExpr,
                        "Parentheses are required for the cast expr",
                        ProblemHighlightType.ERROR,
                        TextRange(castExpr.`as`.startOffsetInParent, castExpr.type.endOffsetInParent),
                        *fixes.toTypedArray()
                    )
                }
            }
        }
}
