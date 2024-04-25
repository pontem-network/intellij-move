package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.valueArguments

class ReplaceWithMethodCallFix(callExpr: MvCallExpr): DiagnosticFix<MvCallExpr>(callExpr) {
    override fun getText(): String = "Replace with method call"

    override fun invoke(project: Project, file: PsiFile, element: MvCallExpr) {
        // can be converted
        val psiFactory = element.project.psiFactory

        val fakeParams = element.valueArguments.drop(1).map { "1" }.toList()
        val methodArgumentList = psiFactory.valueArgumentList(fakeParams)
        val callArguments = element.valueArguments.drop(1)
        for ((argument, callArgument) in methodArgumentList.valueArgumentList.zip(callArguments)) {
            argument.replace(callArgument)
        }

        var selfArgExpr = element.valueArguments.firstOrNull()?.expr ?: return
        if (selfArgExpr is MvBorrowExpr) {
            selfArgExpr = selfArgExpr.expr ?: return
        }

        val dotExpr = psiFactory.expr<MvDotExpr>("1.${element.path.referenceName}()")
        dotExpr.expr.replace(selfArgExpr)
        dotExpr.methodCall?.valueArgumentList?.replace(methodArgumentList)

        element.replace(dotExpr)
    }
}