package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.fixes.AddAcquiresFix
import org.move.ide.presentation.fullnameNoArgs
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvCallable
import org.move.lang.core.psi.ext.isInline
import org.move.lang.core.types.infer.acquiresContext
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyTypeParameter
import org.move.lang.moveProject

class MvMissingAcquiresInspection: MvLocalInspectionTool() {

    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object: MvVisitor() {
            override fun visitCallExpr(o: MvCallExpr) = visitAcquiredTypesOwner(o)
            override fun visitMethodCall(o: MvMethodCall) = visitAcquiredTypesOwner(o)
            override fun visitIndexExpr(o: MvIndexExpr) {
                if (!o.project.moveSettings.enableIndexExpr) return
                visitAcquiredTypesOwner(o)
            }

            private fun visitAcquiredTypesOwner(element: MvAcquireTypesOwner) {
                val outerFunction = element.containingFunction ?: return
                if (outerFunction.isInline) return

                val acquiresContext = element.moveProject?.acquiresContext ?: return
                val inference = outerFunction.inference(false)

                val existingTypes = acquiresContext.getFunctionTypes(outerFunction)

                val callAcquiresTypes = when (element) {
                    is MvCallable -> acquiresContext.getCallTypes(element, inference)
                    is MvIndexExpr -> acquiresContext.getIndexExprTypes(element, inference)
                    else -> return
                }

                val existingTypeNames =
                    existingTypes.map { it.fullnameNoArgs() }.toSet()
                val currentModule = outerFunction.module ?: return
                val missingTypes =
                    callAcquiresTypes.mapNotNull { acqTy ->
                        when {
                            // type parameters can be arguments, but only for inline functions
                            acqTy is TyTypeParameter && outerFunction.isInline ->
                                acqTy.origin.takeIf { tyOrigin -> existingTypes.all { tyOrigin != it } }
                            acqTy is TyAdt -> {
                                val belongsToTheSameModule = acqTy.item.containingModule == currentModule
                                if (
                                    belongsToTheSameModule
                                    && acqTy.fullnameNoArgs() !in existingTypeNames
                                ) {
                                    acqTy.item
                                } else {
                                    null
                                }
                            }
                            else -> null
                        }
                    }

                if (missingTypes.isNotEmpty()) {
                    val name = outerFunction.name ?: return
                    val missingNames = missingTypes.mapNotNull { it.name }
                    holder.registerProblem(
                        element,
                        "Function '$name' is not marked as 'acquires ${missingNames.joinToString()}'",
                        ProblemHighlightType.GENERIC_ERROR,
                        AddAcquiresFix(outerFunction, missingNames)
                    )
                }
            }
        }
}
