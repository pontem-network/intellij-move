package org.move.ide.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.move.lang.core.psi.MvRefExpr
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.psi.MvVisitor

class FieldInitShorthandInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitStructLitField(o: MvStructLitField) {
            val assignment = o.structLitFieldAssignment?.expr ?: return
            if (!(assignment is MvRefExpr && assignment.text == o.identifier.text)) return
            holder.registerProblem(
                o,
                "Expression can be simplified",
                ProblemHighlightType.WEAK_WARNING,
                object : LocalQuickFix {
                    override fun getFamilyName(): String = "Use initialization shorthand"

                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        applyShorthandInit(descriptor.psiElement as MvStructLitField)
                    }
                }
            )
        }
    }

    companion object {
        fun applyShorthandInit(field: MvStructLitField) {
            field.structLitFieldAssignment?.delete()
        }
    }
}
