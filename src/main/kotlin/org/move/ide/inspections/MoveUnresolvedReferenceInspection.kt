package org.move.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

class MoveUnresolvedReferenceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : MoveVisitor() {
            override fun visitModuleRef(moduleRef: MoveModuleRef) {
//                 skip this check, as it will be checked in MovePath visitor
                if (moduleRef.ancestorStrict<MovePath>() != null) return

                if (moduleRef.ancestorStrict<MoveImportStatement>() != null) return
                if (moduleRef is MoveFQModuleRef) return

                if (moduleRef.isUnresolved) {
                    holder.registerProblem(
                        moduleRef,
                        "Unresolved module reference: `${moduleRef.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
            }

            override fun visitPath(path: MovePath) {
                if (path.isPrimitiveType()) return
                val moduleRef = path.pathIdent.moduleRef
                if (moduleRef != null) {
                    if (moduleRef is MoveFQModuleRef) return
                    if (moduleRef.isUnresolved) {
                        holder.registerProblem(
                            moduleRef,
                            "Unresolved module reference: `${moduleRef.referenceName}`",
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                        )
                        return
                    }
                }
                if (path.isUnresolved) {
                    val description = when (path.parent) {
                        is MovePathType -> "Unresolved type: `${path.referenceName}`"
                        else -> "Unresolved reference: `${path.referenceName}`"
                    }
                    val highlightedElement = path.referenceNameElement ?: return
                    holder.registerProblem(
                        highlightedElement,
                        description,
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
            }

            override fun visitStructPatField(o: MoveStructPatField) {
                val resolvedStructDef = o.structPat.path.maybeStruct ?: return
                if (!resolvedStructDef.fieldNames.any { it == o.referenceName }) {
                    val highlightedElement = o.referenceNameElement ?: return
                    holder.registerProblem(
                        highlightedElement,
                        "Unresolved field: `${o.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
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
                    holder.registerProblem(
                        highlightedElement,
                        errorMessage,
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
            }
        }
}
