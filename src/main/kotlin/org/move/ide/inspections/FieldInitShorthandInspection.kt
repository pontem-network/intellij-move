package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.getChild

class FieldInitShorthandInspection : MvLocalInspectionTool() {
    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitStructLitField(o: MvStructLitField) {
            val initExpr = o.expr ?: return
            if (!(initExpr is MvRefExpr && initExpr.text == o.identifier.text)) return
            holder.registerProblem(
                o,
                "Expression can be simplified",
                ProblemHighlightType.WEAK_WARNING,
                object : LocalQuickFix {
                    override fun getFamilyName() = "Use initialization shorthand"

                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        val field = descriptor.psiElement as? MvStructLitField ?: return
                        field.getChild(MvElementTypes.COLON)?.delete()
                        field.expr?.delete()
                    }
                }
            )
        }

        override fun visitStructPatField(o: MvStructPatField) {
            val ident = o.identifier ?: return
            val fieldBinding = o.structPatFieldBinding ?: return
            if (ident.text == fieldBinding.pat?.text.orEmpty()) {
                holder.registerProblem(
                    o,
                    "Expression can be simplified",
                    ProblemHighlightType.WEAK_WARNING,
                    object : LocalQuickFix {
                        override fun getFamilyName() = "Use pattern shorthand"

                        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                            val field = descriptor.psiElement as? MvStructPatField ?: return
                            val fieldIdent = field.identifier ?: return
                            field.structPatFieldBinding?.delete()
                            fieldIdent.replace(project.psiFactory.createBindingPat(fieldIdent.text))
                        }
                    }
                )
            }
        }

        override fun visitSchemaLitField(o: MvSchemaLitField) {
            val initExpr = o.expr ?: return
            if (!(initExpr is MvRefExpr && initExpr.text == o.identifier.text)) return
            holder.registerProblem(
                o,
                "Expression can be simplified",
                ProblemHighlightType.WEAK_WARNING,
                object : LocalQuickFix {
                    override fun getFamilyName() = "Use initialization shorthand"

                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        val field = descriptor.psiElement as? MvSchemaLitField
                            ?: return
                        field.getChild(MvElementTypes.COLON)?.delete()
                        field.expr?.delete()
                    }
                }
            )

        }
    }
}
