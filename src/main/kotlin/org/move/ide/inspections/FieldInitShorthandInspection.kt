package org.move.ide.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.MoveVisitor

class FieldInitShorthandInspection : MoveLocalInspectionTool() {
    override fun buildMoveVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MoveVisitor() {
        override fun visitStructLiteralField(o: MoveStructLiteralField) {
            val assignment = o.structLiteralFieldAssignment?.expr ?: return
            if (!(assignment is MoveRefExpr && assignment.text == o.identifier.text)) return
            holder.registerProblem(
                o,
                "Expression can be simplified",
                ProblemHighlightType.WEAK_WARNING,
                object : LocalQuickFix {
                    override fun getFamilyName(): String = "Use initialization shorthand"

                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        applyShorthandInit(descriptor.psiElement as MoveStructLiteralField)
                    }
                }
            )
        }
    }

    companion object {
        fun applyShorthandInit(field: MoveStructLiteralField) {
            field.structLiteralFieldAssignment?.delete()
        }
    }
}
