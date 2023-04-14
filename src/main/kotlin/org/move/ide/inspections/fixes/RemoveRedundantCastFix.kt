package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType

class RemoveRedundantCastFix(castExpr: MvCastExpr) : DiagnosticFix<MvCastExpr>(castExpr) {
    override fun getFamilyName(): String = "Remove redundant cast"
    override fun getText(): String = "Remove redundant cast"

    override fun stillApplicable(project: Project, file: PsiFile, element: MvCastExpr): Boolean {
//        val itemContext = element.itemContext(false)
        val inference = element.inference(false) ?: return false
        val elementExprTy = inference.getExprType(element.expr)
//        val elementExprTy = element.expr.getInferenceType(false)
//        val elementExprTy = inferExprTy(element.expr, inferenceCtx)
        return elementExprTy == element.type.loweredType(false)
//        return elementExprTy == itemContext.rawType(element.type)
    }

    override fun invoke(project: Project, file: PsiFile, element: MvCastExpr) {
        element.replace(element.expr)
    }


}
