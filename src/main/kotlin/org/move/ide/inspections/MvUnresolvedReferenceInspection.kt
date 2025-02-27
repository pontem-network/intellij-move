package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.cli.settings.isDebugModeEnabled
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.imports.AutoImportFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.PathKind.*
import org.move.lang.core.resolve.pathKind
import org.move.lang.core.types.ty.TyUnknown

class MvUnresolvedReferenceInspection: MvLocalInspectionTool() {

    override val isSyntaxOnly get() = false

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

            val pathKind = path.pathKind()
            when (pathKind) {
                is NamedAddressOrUnqualifiedPath, is NamedAddress, is ValueAddress -> return
                is UnqualifiedPath -> tryMultiResolveOrRegisterError(path, holder)
                is QualifiedPath -> {
                    when (pathKind) {
                        is QualifiedPath.ModuleItemOrEnumVariant,
                        is QualifiedPath.FQModuleItem,
                        is QualifiedPath.UseGroupItem,
                        is QualifiedPath.ModuleOrItem -> {
                            val qualifier = pathKind.qualifier
                            // qualifier is unresolved, no need to resolve current path
                            if (qualifier.reference?.resolve() == null) return
                        }
                        else -> Unit
                    }
                    tryMultiResolveOrRegisterError(path, holder)
                }
            }
        }

        override fun visitPatField(patField: MvPatField) {
            if (patField.isMsl() && !isDebugModeEnabled()) return

            // checked in another method
            if (patField.patFieldFull != null) return
            // pat struct unresolved, do not highlight fields
            if (patField.patStruct.path.reference?.resolve() == null) return

            patField.patBinding?.let { tryMultiResolveOrRegisterError(it, holder) }
        }

        override fun visitPatFieldFull(patFieldFull: MvPatFieldFull) {
            if (patFieldFull.isMsl() && !isDebugModeEnabled())
                return
            // pat struct unresolved, do not highlight fields
            if (patFieldFull.patStruct.path.reference?.resolve() == null) return

            tryMultiResolveOrRegisterError(patFieldFull, holder)
        }

        override fun visitStructLitField(litField: MvStructLitField) {
            if (litField.isMsl() && !isDebugModeEnabled()) {
                return
            }
            tryMultiResolveOrRegisterError(litField, holder)
        }

        override fun visitSchemaLitField(field: MvSchemaLitField) {
            if (field.isShorthand) {
                val resolvedItems = field.reference.multiResolve()
                val fieldBinding = resolvedItems
                    .find { it is MvPatBinding && it.bindingTypeOwner is MvSchemaFieldStmt }
                if (fieldBinding == null) {
                    holder.registerProblem(
                        field.referenceNameElement,
                        "Unresolved field: `${field.referenceName}`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
                }
                val letBinding = resolvedItems.find { it is MvPatBinding }
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

        override fun visitFieldLookup(o: MvFieldLookup) = checkMethodOrField(o)

        override fun visitMethodCall(methodCall: MvMethodCall) {
            if (!methodCall.project.moveSettings.enableReceiverStyleFunctions) return

            checkMethodOrField(methodCall)
        }

        private fun checkMethodOrField(methodOrField: MvMethodOrField) {
            val msl = methodOrField.isMsl()
            if (msl && !isDebugModeEnabled()) return

            // no error if receiver item is unknown (won't proc if unknown is nested)
            if (methodOrField.inferReceiverTy(msl).derefIfNeeded() is TyUnknown) return

            tryMultiResolveOrRegisterError(methodOrField, holder)
        }
    }

    private fun tryMultiResolveOrRegisterError(referenceElement: MvReferenceElement, holder: ProblemsHolder) {
        // no errors in pragmas
        if (referenceElement.hasAncestor<MvPragmaSpecStmt>()) return

        val reference = referenceElement.reference ?: return

        val resolveVariants = reference.multiResolve()
        if (resolveVariants.size == 1) return

        val referenceName = referenceElement.referenceName ?: return
        val parent = referenceElement.parent
        val itemType = when {
            parent is MvPathType -> "type"
            parent is MvCallExpr -> "function"
            parent is MvPatField -> "field"
            referenceElement is MvFieldLookup -> "field"
            referenceElement is MvStructLitField -> "field"
            else -> "reference"
        }
        val highlightedElement = referenceElement.referenceNameElement ?: referenceElement

        val description = "Unresolved $itemType: `$referenceName`"
        if (resolveVariants.isEmpty()) {
            val fix = (referenceElement as? MvPath)?.let {
                val candidates = AutoImportFix.findApplicableContext(referenceElement)?.candidates.orEmpty()
                if (candidates.isNotEmpty()) AutoImportFix(referenceElement) else null
            }
            holder.registerProblem(
                highlightedElement,
                description,
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                *listOfNotNull(fix).toTypedArray()
            )
        } else {
            check(resolveVariants.size > 1)
            // multiple resolution is expected for shorthands
            if (referenceElement is MvStructLitField && referenceElement.isShorthand) return
            holder.registerProblem(
                highlightedElement,
                "$description. Multiple items are found, resolution is ambiguous",
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
            )
        }
    }
}
