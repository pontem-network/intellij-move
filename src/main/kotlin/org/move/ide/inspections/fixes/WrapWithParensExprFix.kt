package org.move.ide.inspections.fixes

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import org.move.ide.inspections.MvLocalQuickFixOnPsiElement
import org.move.lang.core.psi.*

class WrapWithParensExprFix(
    castExpr: MvCastExpr,
    val atomExprPointer: SmartPsiElementPointer<MvExpr>?,
) : MvLocalQuickFixOnPsiElement<MvCastExpr>(castExpr) {
    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

    override fun availableInBatchMode(): Boolean = false
    override fun getFamilyName(): String = "Add parentheses to the cast expr"

    override fun getText(): String {
        val atomExpr = this.atomExpr()
        val atomExprOffset = atomExpr.textOffset - startElement.textOffset
        val replacementText =
            startElement.text.substring(atomExprOffset, startElement.textLength)
        return "Add parentheses to `$replacementText`"
    }

    override fun stillApplicable(project: Project, file: PsiFile, element: MvCastExpr): Boolean =
        element.parent !is MvParensExpr

    override fun invoke(project: Project, file: PsiFile, element: MvCastExpr) {
        val psiFactory = project.psiFactory
        val atomExpr = this.atomExpr()

        // 1. create new CastExpr of atomExpr with type cast
        val atomCastExpr = element.copy() as MvCastExpr
        atomCastExpr.expr.replace(atomExpr)

        // 2. wrap this cast expr with parens expr
        val parensExpr = wrapWithParensExpr(psiFactory, atomCastExpr)

        // 3. replace atomExpr with this cast expr
        atomExpr.replace(parensExpr)

        // 4. replace original cast expr with its `expr`
        element.replace(element.expr)
    }

    private fun atomExpr(): MvExpr = atomExprPointer?.element ?: (startElement as MvCastExpr).expr
}

private fun wrapWithParensExpr(psiFactory: MvPsiFactory, expr: MvExpr): MvParensExpr {
    val parensExpr = psiFactory.expr<MvParensExpr>("(dummy_ident)")
    parensExpr.expr?.replace(expr)
    return expr.replace(parensExpr) as MvParensExpr
}
