package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

class UnresolvedReferenceInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitModuleRef(moduleRef: MvModuleRef) {
            if (isSpecElement(moduleRef)) return
            // skip this check, as it will be checked in MvPath visitor
            if (moduleRef.ancestorStrict<MvPath>() != null) return

            if (moduleRef.ancestorStrict<MvImportStatement>() != null) return
            if (moduleRef is MvFQModuleRef) return

            if (moduleRef.isUnresolved) {
                holder.registerProblem(
                    moduleRef,
                    "Unresolved module reference: `${moduleRef.referenceName}`",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }

        override fun visitPath(path: MvPath) {
            if (isSpecElement(path)) return
            if (path.isPrimitiveType()) return
            val moduleRef = path.pathIdent.moduleRef
            if (moduleRef != null) {
                if (moduleRef is MvFQModuleRef) return
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
                    is MvPathType -> "Unresolved type: `${path.referenceName}`"
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

        override fun visitStructPatField(o: MvStructPatField) {
            if (isSpecElement(o)) return
            val resolvedStructDef = o.structPat.path.maybeStruct ?: return
            if (!resolvedStructDef.fieldNames.any { it == o.referenceName }) {
                holder.registerProblem(
                    o.referenceNameElement,
                    "Unresolved field: `${o.referenceName}`",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }

        override fun visitStructLitField(o: MvStructLitField) {
            if (isSpecElement(o)) return
            if (o.isUnresolved) {
                val errorMessage =
                    if (o.isShorthand)
                        "Unresolved reference: `${o.referenceName}`"
                    else
                        "Unresolved field: `${o.referenceName}`"
                holder.registerProblem(
                    o.referenceNameElement,
                    errorMessage,
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }
    }

    private fun isSpecElement(element: MvElement): Boolean {
        return element.isInsideSpecBlock()
    }
}
