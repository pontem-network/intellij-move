package org.move.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.fieldNames
import org.move.lang.core.psi.ext.isIdentifierOnly

class MoveUnresolvedReferenceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : MoveVisitor() {

            override fun visitModuleRef(moduleRef: MoveModuleRef) {
                if (moduleRef.ancestorStrict<MoveImportStatement>() != null) return
                if (moduleRef is MoveFullyQualifiedModuleRef) return

                if (moduleRef.isUnresolved) {
                    holder.registerProblem(
                        moduleRef,
                        "Unresolved module reference: `${moduleRef.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                }
            }

            override fun visitQualPath(qualPath: MoveQualPath) {
                val refElement = qualPath.parent
                if (refElement !is MoveQualPathReferenceElement) return

                if (refElement.isUnresolved && qualPath.isIdentifierOnly) {
                    val highlightedElement = refElement.referenceNameElement
                    holder.registerProblem(
                        highlightedElement,
                        "Unresolved reference: `${refElement.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                }
            }

            override fun visitStructPatField(o: MoveStructPatField) {
                val resolvedStructDef = o.ancestorStrict<MoveStructPat>()?.referredStructDef ?: return
                if (!resolvedStructDef.fieldNames.any { it == o.referenceName }) {
                    val highlightedElement = o.referenceNameElement
                    holder.registerProblem(
                        highlightedElement,
                        "Unresolved field: `${o.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                }
            }

            override fun visitStructLiteralField(o: MoveStructLiteralField) {
                val resolvedStructDef = o.ancestorStrict<MoveStructLiteralExpr>()?.referredStructDef ?: return
                if (!resolvedStructDef.fieldNames.any { it == o.referenceName }) {
                    val highlightedElement = o.referenceNameElement
                    holder.registerProblem(
                        highlightedElement,
                        "Unresolved field: `${o.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                }
            }
        }
}