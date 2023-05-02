package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.ide.presentation.name
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvParensExpr
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.types.ty.Ty

class AddCastFix(expr: MvExpr, val castType: Ty) : DiagnosticFix<MvExpr>(expr) {
    override fun availableInBatchMode(): Boolean = false

    override fun getText(): String = "Cast to '${castType.name()}'"

    override fun invoke(project: Project, file: PsiFile, element: MvExpr) {
        val psiFactory = project.psiFactory

        val parensExpr = psiFactory.expr<MvParensExpr>("(1 as ${castType.name()})");
        val oldExpr = element.copy()
        val newParensExpr = element.replace(parensExpr) as MvParensExpr

        val litExpr = (newParensExpr.expr as MvCastExpr).expr
        litExpr.replace(oldExpr)
    }

}
