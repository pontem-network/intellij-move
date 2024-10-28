package org.move.ide.inspections.compilerV2

import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.MOVE_ARITHMETIC_BINARY_OPS
import org.move.lang.core.psi.MvAssignmentExpr
import org.move.lang.core.psi.MvBinaryExpr
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.operator
import org.move.lang.core.psi.psiFactory

class MvReplaceWithCompoundAssignmentInspection:
    Move2OnlyInspectionBase<MvAssignmentExpr>(MvAssignmentExpr::class.java) {

    override fun visitTargetElement(element: MvAssignmentExpr, holder: ProblemsHolder, isOnTheFly: Boolean) {
        val lhsExpr = element.expr
        val initializerExpr = element.initializer.expr ?: return
        if (initializerExpr is MvBinaryExpr
            && initializerExpr.operator.elementType in MOVE_ARITHMETIC_BINARY_OPS
        ) {
            // take lhs of binary plus expr
            val argumentExpr = initializerExpr.left
            if (PsiEquivalenceUtil.areElementsEquivalent(lhsExpr, argumentExpr)) {
                val op = initializerExpr.operator.text
                holder.registerProblem(
                    element,
                    "Can be replaced with compound assignment",
                    WEAK_WARNING,
                    ReplaceWithCompoundAssignmentFix(element, op)
                )
            }
        }
    }

    class ReplaceWithCompoundAssignmentFix(assignmentExpr: MvAssignmentExpr, val op: String):
        DiagnosticFix<MvAssignmentExpr>(assignmentExpr) {

        override fun getText(): String = "Replace with compound assignment expr"

        override fun invoke(project: Project, file: PsiFile, element: MvAssignmentExpr) {
            val lhsExpr = element.expr
            val rhsExpr = (element.initializer.expr as? MvBinaryExpr)?.right ?: return

            val psiFactory = project.psiFactory
            val assignBinExpr = psiFactory.expr<MvBinaryExpr>("x $op= 1")
            assignBinExpr.left.replace(lhsExpr)
            assignBinExpr.right?.replace(rhsExpr)

            element.replace(assignBinExpr)
        }
    }
}