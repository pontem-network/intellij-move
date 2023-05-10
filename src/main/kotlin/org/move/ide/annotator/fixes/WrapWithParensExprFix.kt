package org.move.ide.annotator.fixes

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticIntentionFix
import org.move.lang.core.psi.*

class WrapWithParensExprFix(castExpr: MvCastExpr) : DiagnosticIntentionFix<MvCastExpr>(castExpr) {
    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY
    override fun getFamilyName(): String = "Wrap cast with ()"
    override fun getText(): String = "Wrap cast with ()"

    override fun stillApplicable(project: Project, file: PsiFile, element: MvCastExpr): Boolean =
        element.parent !is MvParensExpr

    override fun invoke(project: Project, file: PsiFile, element: MvCastExpr) {
        val psiFactory = project.psiFactory
        wrapWithParensExpr(psiFactory, element)
    }
}

private fun wrapWithParensExpr(psiFactory: MvPsiFactory, expr: MvExpr): MvParensExpr {
    val parensExpr = psiFactory.expr<MvParensExpr>("(dummy_ident)")
    parensExpr.expr?.replace(expr)
    return expr.replace(parensExpr) as MvParensExpr
}
