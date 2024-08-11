package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.fixes.FieldShorthandFix
import org.move.lang.core.psi.*

class FieldInitShorthandInspection : MvLocalInspectionTool() {
    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitStructLitField(field: MvStructLitField) {
            val initExpr = field.expr ?: return
            if (!(initExpr is MvRefExpr && initExpr.text == field.identifier.text)) return
            holder.registerProblem(
                field,
                "Expression can be simplified",
                ProblemHighlightType.WEAK_WARNING,
                FieldShorthandFix.StructLit(field),
            )
        }

        override fun visitFieldPatFull(fieldPatFull: MvFieldPatFull) {
            val fieldName = fieldPatFull.referenceName
            val binding = fieldPatFull.pat as? MvBindingPat ?: return
            if (fieldName == binding.text) {
                holder.registerProblem(
                    fieldPatFull,
                    "Expression can be simplified",
                    ProblemHighlightType.WEAK_WARNING,
                    FieldShorthandFix.StructPat(fieldPatFull)
                )
            }
        }

//        override fun visitFieldPat(field: MvFieldPat) {
//            val ident = field.identifier ?: return
//            val fieldBinding = field.fieldPatBinding ?: return
//            if (ident.text == fieldBinding.pat.text.orEmpty()) {
//                holder.registerProblem(
//                    field,
//                    "Expression can be simplified",
//                    ProblemHighlightType.WEAK_WARNING,
//                    FieldShorthandFix.StructPat(field)
//                )
//            }
//        }

        override fun visitSchemaLitField(schemaField: MvSchemaLitField) {
            val initExpr = schemaField.expr ?: return
            if (!(initExpr is MvRefExpr && initExpr.text == schemaField.identifier.text)) return
            holder.registerProblem(
                schemaField,
                "Expression can be simplified",
                ProblemHighlightType.WEAK_WARNING,
                FieldShorthandFix.Schema(schemaField)
            )

        }
    }
}
