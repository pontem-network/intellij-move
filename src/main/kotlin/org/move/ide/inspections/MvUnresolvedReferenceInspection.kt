package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.cli.settings.isDebugModeEnabled
import org.move.ide.inspections.imports.AutoImportFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve2.PathKind.*
import org.move.lang.core.resolve2.pathKind
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyUnknown

class MvUnresolvedReferenceInspection: MvLocalInspectionTool() {

    var ignoreWithoutQuickFix: Boolean = false

    override val isSyntaxOnly get() = false

    private fun ProblemsHolder.registerUnresolvedReferenceError(path: MvPath) {
        // no errors in pragmas
        if (path.hasAncestor<MvPragmaSpecStmt>()) return

        val candidates = AutoImportFix.findApplicableContext(path)?.candidates.orEmpty()
        if (candidates.isEmpty() && ignoreWithoutQuickFix) return

        val referenceName = path.referenceName ?: return
        val parent = path.parent
        val description = when (parent) {
            is MvPathType -> "Unresolved type: `$referenceName`"
            is MvCallExpr -> "Unresolved function: `$referenceName`"
            else -> "Unresolved reference: `$referenceName`"
        }

        val highlightedElement = path.referenceNameElement ?: path
        val fix = if (candidates.isNotEmpty()) AutoImportFix(path) else null
        registerProblem(
            highlightedElement,
            description,
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
            *listOfNotNull(fix).toTypedArray()
        )
    }

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object: MvVisitor() {

        override fun visitPath(path: MvPath) {
            // skip specs in non-dev mode, too many false-positives
            if (path.isMslScope && !isDebugModeEnabled()) return
            if (path.isMslScope && path.isSpecPrimitiveType()) return
            if (path.isUpdateFieldArg2) return
            if (path.isPrimitiveType()) return
            // destructuring assignment like `Coin { val1: _ } = get_coin()`
            if (path.textMatches("_") && path.isInsideAssignmentLhs()) return
            // assert macro
            if (path.text == "assert") return
            // attribute values are special case
            if (path.hasAncestor<MvAttrItem>()) return

            val pathReference = path.reference ?: return
            val pathKind = path.pathKind()
            when (pathKind) {
                is NamedAddress, is ValueAddress -> return
                is UnqualifiedPath -> {
                    if (pathReference.resolve() == null) {
                        holder.registerUnresolvedReferenceError(path)
                    }
                }
                is QualifiedPath -> {
                    if (pathKind !is QualifiedPath.Module) {
                        val qualifier = pathKind.qualifier
                        // qualifier is unresolved, no need to resolve current path
                        if (qualifier.reference?.resolve() == null) return
                    }
                    if (pathReference.resolve() == null) {
                        holder.registerUnresolvedReferenceError(path)
                    }
                }
            }
        }

        override fun visitFieldPat(patField: MvFieldPat) {
            if (patField.isMsl() && !isDebugModeEnabled()) {
                return
            }
            val resolvedStructDef = patField.structPat.path.maybeStruct ?: return
            if (!resolvedStructDef.fieldNames.any { it == patField.referenceName }) {
                holder.registerProblem(
                    patField.referenceNameElement,
                    "Unresolved field: `${patField.referenceName}`",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }

        override fun visitStructLitField(litField: MvStructLitField) {
            if (litField.isMsl() && !isDebugModeEnabled()) {
                return
            }
            if (litField.isShorthand) {
                val resolvedItems = litField.reference.multiResolve()
                val resolvedStructField = resolvedItems.find { it is MvNamedFieldDecl }
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
            if (dotExpr.isMsl() && !isDebugModeEnabled()) {
                return
            }
            val receiverTy = dotExpr.inference(false)?.getExprType(dotExpr.expr)
            // disable inspection is object is unresolved
            if (receiverTy is TyUnknown) return

            val dotField = dotExpr.structDotField ?: return
            if (dotField.unresolved) {
                holder.registerProblem(
                    dotField.referenceNameElement,
                    "Unresolved field: `${dotField.referenceName}`",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }

//        override fun visitModuleUseSpeck(o: MvModuleUseSpeck) {
//            val moduleRef = o.fqModuleRef ?: return
//            if (!moduleRef.resolvable) {
//                val refNameElement = moduleRef.referenceNameElement ?: return
//                holder.registerProblem(
//                    refNameElement,
//                    "Unresolved reference: `${refNameElement.text}`",
//                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
//                )
//            }
//        }

//        override fun visitItemUseSpeck(o: MvItemUseSpeck) {
//            val moduleRef = o.fqModuleRef
//            if (!moduleRef.resolvable) {
//                val refNameElement = moduleRef.referenceNameElement ?: return
//                holder.registerProblem(
//                    refNameElement,
//                    "Unresolved reference: `${refNameElement.text}`",
//                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
//                )
//                return
//            }
//            val useItems = o.descendantsOfType<MvUseItem>()
//            for (useItem in useItems) {
//                if (!useItem.resolvable) {
//                    val refNameElement = useItem.referenceNameElement
//                    holder.registerProblem(
//                        refNameElement,
//                        "Unresolved reference: `${refNameElement.text}`",
//                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
//                    )
//                }
//            }
//        }
    }
}
