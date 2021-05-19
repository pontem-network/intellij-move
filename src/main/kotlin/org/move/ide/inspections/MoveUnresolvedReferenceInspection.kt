package org.move.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

fun registerProblem(holder: ProblemsHolder, element: PsiElement, description: String) {
    holder.registerProblem(
        element,
        description,
        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
    )
}

class MoveUnresolvedReferenceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : MoveVisitor() {
            override fun visitModuleRef(moduleRef: MoveModuleRef) {
                if (moduleRef.ancestorStrict<MoveImportStatement>() != null) return
                if (moduleRef is MoveFullyQualifiedModuleRef) return

                if (moduleRef.isUnresolved) {
                    registerProblem(
                        holder,
                        moduleRef,
                        "Unresolved module reference: `${moduleRef.referenceName}`"
                    )
                }
            }

            override fun visitQualPath(qualPath: MoveQualPath) {
                val refElement = qualPath.parent
                if (refElement !is MoveQualPathReferenceElement) return

                if (refElement.isUnresolved && qualPath.isIdentifierOnly) {
                    val description = when (refElement) {
                        is MoveQualPathType -> "Unresolved type: `${refElement.referenceName}`"
                        else -> "Unresolved reference: `${refElement.referenceName}`"
                    }
                    val highlightedElement = refElement.referenceNameElement ?: return
                    registerProblem(
                        holder,
                        highlightedElement,
                        description
                    )
//                    holder.registerProblem(
//                        highlightedElement,
//                        "Unresolved reference: `${refElement.referenceName}`",
//                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
//                    )
                }
            }

            override fun visitStructPatField(o: MoveStructPatField) {
                val resolvedStructDef = o.structPat.referredStructDef ?: return
                if (!resolvedStructDef.fieldNames.any { it == o.referenceName }) {
                    val highlightedElement = o.referenceNameElement ?: return
                    registerProblem(
                        holder,
                        highlightedElement,
                        "Unresolved field: `${o.referenceName}`"
                    )
//                    holder.registerProblem(
//                        highlightedElement,
//                        "Unresolved field: `${o.referenceName}`",
//                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
//                    )
                }
            }

            override fun visitStructLiteralField(o: MoveStructLiteralField) {
                if (o.isUnresolved) {
                    val highlightedElement = o.referenceNameElement ?: return
                    val errorMessage =
                        if (o.isShorthand)
                            "Unresolved reference: `${o.referenceName}`"
                        else
                            "Unresolved field: `${o.referenceName}`"
                    registerProblem(
                        holder,
                        highlightedElement,
                        errorMessage
                    )
//                    holder.registerProblem(
//                        highlightedElement,
//                        errorMessage,
//                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
//                    )
                }
            }
        }
}
