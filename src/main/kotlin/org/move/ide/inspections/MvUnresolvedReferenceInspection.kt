package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.descendantsOfType
import org.move.ide.inspections.imports.AutoImportFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.MvReferenceElement
import org.move.lang.core.types.infer.inferDotExprStructTy
import org.move.lang.core.types.infer.ownerInferenceCtx

class MvUnresolvedReferenceInspection : MvLocalInspectionTool() {

    var ignoreWithoutQuickFix: Boolean = false

    override val isSyntaxOnly get() = false

    private fun ProblemsHolder.registerProblem(
        element: MvReferenceElement
    ) {
        val candidates = AutoImportFix.findApplicableContext(element)?.candidates.orEmpty()
        if (candidates.isEmpty() && ignoreWithoutQuickFix) return

        val referenceName = element.referenceName
        val description =
            if (referenceName == null) "Unresolved reference" else "Unresolved reference: `$referenceName`"
        val highlightedElement = element.referenceNameElement ?: element
        val fix = if (candidates.isNotEmpty()) AutoImportFix(element) else null
        registerProblem(
            highlightedElement,
            description,
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
            *listOfNotNull(fix).toTypedArray()
        )
    }

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitModuleRef(moduleRef: MvModuleRef) {
            if (moduleRef.isMsl()) return

            // skip this check, as it will be checked in MvPath visitor
            if (moduleRef.ancestorStrict<MvPath>() != null) return

            if (moduleRef.ancestorStrict<MvUseStmt>() != null) return
            if (moduleRef is MvFQModuleRef) return

            if (moduleRef.unresolved) {
                holder.registerProblem(moduleRef)
            }
        }

        override fun visitPath(path: MvPath) {
            if (path.isMsl()) return
            if (path.isMsl() && path.isResult) return
            if (path.isUpdateFieldArg2) return
            if (path.isPrimitiveType()) return
            if (path.isMsl() && path.isSpecPrimitiveType()) return
            if (path.isInsideAssignmentLeft()) return
            if (path.text == "assert") return

            val moduleRef = path.moduleRef
            if (moduleRef != null) {
                if (moduleRef is MvFQModuleRef) return
                if (moduleRef.unresolved) {
                    holder.registerProblem(moduleRef)
                    return
                }
            }
            if (path.unresolved) {
                holder.registerProblem(path)
            }
        }

        override fun visitStructPatField(o: MvStructPatField) {
            if (o.isMsl()) return
            val resolvedStructDef = o.structPat.path.maybeStruct ?: return
            if (!resolvedStructDef.fieldNames.any { it == o.referenceName }) {
                holder.registerProblem(
                    o.referenceNameElement,
                    "Unresolved field: `${o.referenceName}`",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }

        override fun visitStructLitField(litField: MvStructLitField) {
            if (litField.isMsl()) return
            if (litField.isShorthand) {
                val resolvedItems = litField.reference.multiResolve()
                val resolvedStructField = resolvedItems.find { it is MvStructField }
                if (resolvedStructField == null) {
                    holder.registerProblem(
                        litField.referenceNameElement,
                        "Unresolved field: `${litField.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
                val resolvedBinding = resolvedItems.find { it is MvBindingPat }
                if (resolvedBinding == null) {
                    holder.registerProblem(
                        litField.referenceNameElement,
                        "Unresolved reference: `${litField.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
            } else {
                if (litField.reference.resolve() == null) {
                    holder.registerProblem(
                        litField.referenceNameElement,
                        "Unresolved field: `${litField.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
            }
        }

        override fun visitSchemaLitField(field: MvSchemaLitField) {
            if (field.isShorthand) {
                val resolvedItems = field.reference.multiResolve()
                val fieldBinding = resolvedItems.find { it is MvBindingPat && it.owner is MvSchemaFieldStmt }
                if (fieldBinding == null) {
                    holder.registerProblem(
                        field.referenceNameElement,
                        "Unresolved field: `${field.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
                val letBinding = resolvedItems.find { it is MvBindingPat }
                if (letBinding == null) {
                    holder.registerProblem(
                        field.referenceNameElement,
                        "Unresolved reference: `${field.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
            } else {
                if (field.reference.resolve() == null) {
                    holder.registerProblem(
                        field.referenceNameElement,
                        "Unresolved field: `${field.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
            }
        }

        override fun visitDotExpr(dotExpr: MvDotExpr) {
            if (dotExpr.isMsl()) return

            val inferenceCtx = dotExpr.ownerInferenceCtx(false) ?: return
            inferDotExprStructTy(dotExpr, inferenceCtx) ?: return

            val dotField = dotExpr.structDotField
            if (!dotField.resolvable) {
                holder.registerProblem(
                    dotField.referenceNameElement,
                    "Unresolved field: `${dotField.referenceName}`",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }

        override fun visitModuleUseSpeck(o: MvModuleUseSpeck) {
            val moduleRef = o.fqModuleRef ?: return
            if (!moduleRef.resolvable) {
                val refNameElement = moduleRef.referenceNameElement ?: return
                holder.registerProblem(
                    refNameElement,
                    "Unresolved reference: `${refNameElement.text}`",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }

        override fun visitItemUseSpeck(o: MvItemUseSpeck) {
            val moduleRef = o.fqModuleRef
            if (!moduleRef.resolvable) {
                val refNameElement = moduleRef.referenceNameElement ?: return
                holder.registerProblem(
                    refNameElement,
                    "Unresolved reference: `${refNameElement.text}`",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
                return
            }
            val useItems = o.descendantsOfType<MvUseItem>()
            for (useItem in useItems) {
                if (!useItem.resolvable) {
                    val refNameElement = useItem.referenceNameElement
                    holder.registerProblem(
                        refNameElement,
                        "Unresolved reference: `${refNameElement.text}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
            }
        }
    }
}
