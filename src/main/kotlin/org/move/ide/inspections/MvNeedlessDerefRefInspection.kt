package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NonNls
import org.move.lang.core.psi.MvBorrowExpr
import org.move.lang.core.psi.MvDerefExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvParensExpr
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.unwrap

class MvNeedlessDerefRefInspection: MvLocalInspectionTool() {
    override fun getID(): @NonNls String = LINT_ID

    override fun buildMvVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): MvVisitor = object: MvVisitor() {

        override fun visitDerefExpr(o: MvDerefExpr) {
            val a = 1
            val innerExpr = o.innerExpr
            if (innerExpr is MvBorrowExpr) {
                if (innerExpr.expr == null) return
                holder.registerProblem(
                    o,
                    "Needless pair of `*` and `&` operators: consider removing them",
                    ProblemHighlightType.WEAK_WARNING,
                    RemoveRefDerefFix(o)
                )
            }
        }
    }

    private class RemoveRefDerefFix(element: MvDerefExpr): DiagnosticFix<MvDerefExpr>(element) {

        override fun getText(): String = "Remove needless `*`, `&` operators"

        override fun invoke(project: Project, file: PsiFile, element: MvDerefExpr) {
            val borrowExpr = element.innerExpr as? MvBorrowExpr ?: return
            val itemExpr = borrowExpr.expr ?: return
            element.replace(itemExpr)
        }
    }

    companion object {
        const val LINT_ID = "needless_deref_ref"
    }
}

private val MvDerefExpr.innerExpr: MvExpr? get() {
    val expr = this.expr
    return if (expr !is MvParensExpr) expr else expr.unwrap()
}
