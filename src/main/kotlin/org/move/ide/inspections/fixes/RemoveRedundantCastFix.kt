package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.infer.inferenceContext

class RemoveRedundantCastFix(castExpr: MvCastExpr) : DiagnosticFix<MvCastExpr>(castExpr) {
    override fun getFamilyName(): String = "Remove redundant cast"
    override fun getText(): String = "Remove redundant cast"

    override fun stillApplicable(project: Project, file: PsiFile, element: MvCastExpr): Boolean {
        val inferenceCtx = element.inferenceContext(false)
        val elementExprTy = inferExprTy(element.expr, inferenceCtx)
        return elementExprTy == inferenceCtx.getTypeTy(element.type)
    }

    override fun invoke(project: Project, file: PsiFile, element: MvCastExpr) {
        element.replace(element.expr)
    }


}
