package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.core.psi.*

sealed class FieldShorthandFix<T : MvElement>(field: T) : DiagnosticFix<T>(field) {

    class StructLit(field: MvStructLitField) : FieldShorthandFix<MvStructLitField>(field) {
        override fun getText(): String = "Use initialization shorthand"

        override fun invoke(project: Project, file: PsiFile, element: MvStructLitField) {
            element.colon?.delete()
            element.expr?.delete()
        }
    }

    class StructPat(field: MvStructPatField) : FieldShorthandFix<MvStructPatField>(field) {
        override fun getText(): String = "Use pattern shorthand"

        override fun invoke(project: Project, file: PsiFile, element: MvStructPatField) {
            val fieldIdent = element.identifier ?: return
            element.structPatFieldBinding?.delete()
            fieldIdent.replace(project.psiFactory.bindingPat(fieldIdent.text))
        }
    }

    class Schema(field: MvSchemaLitField) : FieldShorthandFix<MvSchemaLitField>(field) {
        override fun getText(): String = "Use initialization shorthand"

        override fun invoke(project: Project, file: PsiFile, element: MvSchemaLitField) {
            element.colon?.delete()
            element.expr?.delete()
        }
    }
}
