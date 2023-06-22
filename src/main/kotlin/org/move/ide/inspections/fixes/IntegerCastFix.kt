package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.ide.presentation.name
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvParensExpr
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyInteger

sealed class IntegerCastFix<T: MvExpr>(expr: T) : DiagnosticFix<T>(expr) {

    override fun availableInBatchMode(): Boolean = false
    override fun getFamilyName(): String = "Fix integer cast"

    class AddCast(expr: MvExpr, val castType: TyInteger): IntegerCastFix<MvExpr>(expr) {

        override fun getText(): String = "Cast to '${castType.name()}'"

        override fun invoke(project: Project, file: PsiFile, element: MvExpr) {
            val psiFactory = project.psiFactory

            val parensExpr = psiFactory.expr<MvParensExpr>("(1 as ${castType.name()})")
            val oldExpr = element.copy()
            val newParensExpr = element.replace(parensExpr) as MvParensExpr

            val litExpr = (newParensExpr.expr as MvCastExpr).expr
            litExpr.replace(oldExpr)
        }
    }

    class RemoveCast(expr: MvCastExpr): IntegerCastFix<MvCastExpr>(expr) {
        override fun getText(): String {
            val castTy = this.targetElement?.type?.loweredType(false)
            if (castTy != null) {
                return "Remove '${castTy.name()}' cast"
            } else {
                return "Remove cast"
            }
        }

        override fun invoke(
            project: Project,
            file: PsiFile,
            element: MvCastExpr
        ) =
            element.replaceWithChildExpr()
    }

    class ChangeCast(expr: MvCastExpr, val castType: TyInteger): IntegerCastFix<MvCastExpr>(expr) {

        override fun getText(): String = "Change cast type to '${castType.name()}'"

        override fun invoke(
            project: Project,
            file: PsiFile,
            element: MvCastExpr
        ) {
            val newType = project.psiFactory.type(castType.name())
            element.type.replace(newType)
        }
    }
}
