package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.annotator.fixes.RemoveRedundantParenthesesFix
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.MvParensExpr
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType

class RemoveRedundantCastFix(castExpr: MvCastExpr) : DiagnosticFix<MvCastExpr>(castExpr) {

    override fun getText(): String = "Remove redundant cast"

    override fun stillApplicable(project: Project, file: PsiFile, element: MvCastExpr): Boolean {
        val inference = element.inference(false) ?: return false
        val elementExprTy = inference.getExprType(element.expr)
        val typeTy = element.type.loweredType(false)
        return elementExprTy == typeTy
    }

    override fun invoke(
        project: Project,
        file: PsiFile,
        element: MvCastExpr
    ) =
        element.replaceWithChildExpr()
}

fun MvCastExpr.replaceWithChildExpr() {
    val newElement = this.replace(this.expr)
    val parent = newElement.parent
    if (parent is MvParensExpr) {
        RemoveRedundantParenthesesFix(parent).applyFix()
    }
}
