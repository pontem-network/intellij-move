package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.MvLocalQuickFixOnPsiElement
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.ext.inferredExprTy
import org.move.lang.core.types.infer.inferTypeTy
import org.move.lang.core.types.infer.ownerInferenceCtx

class RemoveRedundantCastFix(castExpr: MvCastExpr) : MvLocalQuickFixOnPsiElement<MvCastExpr>(castExpr) {
    override fun getFamilyName(): String = "Remove redundant cast"
    override fun getText(): String = "Remove redundant cast"

    override fun stillApplicable(project: Project, file: PsiFile, element: MvCastExpr): Boolean {
        val inferenceCtx = element.ownerInferenceCtx(false)
            ?: return false
        return element.expr.inferredExprTy() == inferTypeTy(element.type, inferenceCtx)
    }

    override fun invoke(project: Project, file: PsiFile, element: MvCastExpr) {
        element.replace(element.expr)
    }


}
