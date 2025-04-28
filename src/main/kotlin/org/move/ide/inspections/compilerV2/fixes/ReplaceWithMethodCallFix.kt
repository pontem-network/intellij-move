package org.move.ide.inspections.compilerV2.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.path
import org.move.lang.core.psi.ext.typeArguments
import org.move.lang.core.psi.ext.valueArguments
import org.move.stdext.notEmptyOrLet

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
        when (selfArgExpr) {
            // all AtomExpr list, same priority as MvDotExpr
            is MvVectorLitExpr, is MvStructLitExpr, is MvTupleLitExpr, is MvParensExpr, is MvAnnotatedExpr,
            is MvDotExpr, is MvIndexExpr, is MvCallExpr, is MvAssertMacroExpr, is MvPathExpr, is MvLambdaExpr,
            is MvLitExpr, is MvCodeBlockExpr -> {
                // do nothing, those operations priorities are correct without parens
            }
            else -> {
                selfArgExpr = psiFactory.wrapWithParens(selfArgExpr)
            }
        }

        val callPath = element.path ?: return
        val fakeTypeArgs =
            callPath.typeArguments.map { "T" }.toList()
                .notEmptyOrLet { listOf("T") }.joinToString(", ")
        val dotExpr = psiFactory.expr<MvDotExpr>("1.${callPath.referenceName}::<$fakeTypeArgs>()")
        dotExpr.expr.replace(selfArgExpr)

        val typeArgumentList = callPath.typeArgumentList
        if (typeArgumentList != null) {
            val dotExprList = dotExpr.methodCall?.typeArgumentList?.typeArgumentList!!
            for ((dotExprTypeArgument, typeArgument) in dotExprList.zip(typeArgumentList.typeArgumentList)) {
                dotExprTypeArgument.replace(typeArgument)
            }
        } else {
            dotExpr.methodCall?.typeArgumentList?.delete()
        }

        dotExpr.methodCall?.valueArgumentList?.replace(methodArgumentList)

        element.replace(dotExpr)
    }
}