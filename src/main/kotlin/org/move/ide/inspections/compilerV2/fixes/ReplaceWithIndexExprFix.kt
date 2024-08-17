package org.move.ide.inspections.compilerV2.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.argExpr
import org.move.lang.core.psi.ext.argumentExprs
import org.move.lang.core.psi.ext.receiverExpr

class ReplaceWithIndexExprFix(expr: MvExpr): DiagnosticFix<MvExpr>(expr) {
    override fun getText(): String = "Replace with index expr"
    override fun invoke(project: Project, file: PsiFile, element: MvExpr) {
        val derefExpr = element as? MvDerefExpr ?: return
        val callExpr = derefExpr.expr as? MvCallExpr ?: return

        val receiverParamExpr = callExpr.argumentExprs.firstOrNull() ?: return
        val argParamExpr = callExpr.argumentExprs.drop(1).firstOrNull() ?: return

        val indexExpr = project.psiFactory.expr<MvIndexExpr>("v[0]")
        indexExpr.argExpr.replace(argParamExpr)

        val receiverExpr = when (receiverParamExpr) {
            is MvPathExpr, is MvParensExpr -> receiverParamExpr
            is MvBorrowExpr -> receiverParamExpr.expr ?: return
            else -> project.psiFactory.wrapWithParens(receiverParamExpr)
        }
        indexExpr.receiverExpr.replace(receiverExpr)

        element.replace(indexExpr)
    }
}