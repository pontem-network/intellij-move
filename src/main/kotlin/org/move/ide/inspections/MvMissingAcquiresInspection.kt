package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.fixes.AddAcquiresFix
import org.move.ide.presentation.fullnameNoArgs
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isInline
import org.move.lang.core.types.infer.AcquiresTypeContext
import org.move.lang.core.types.infer.MvAcquireTypesOwner
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.visitInnerAcquireTypeOwners
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt

class MvMissingAcquiresVisitor(val holder: ProblemsHolder): MvVisitor() {
    override fun visitFunction(outerFunction: MvFunction) {
        // do not check for inline functions
        if (outerFunction.isInline) return

        val outerModule = outerFunction.module ?: return

        val ctx = AcquiresTypeContext()
        // it's non-inline, so it extracts MvAcquiresType
        val outerAcquiredTypes = ctx.getTypesAcquiredInFun(outerFunction)

        val inference = outerFunction.inference(false)
        visitInnerAcquireTypeOwners(outerFunction) { element ->
            val elementAcquiredTys = ctx.getAcquiredTypes(element, inference)
            checkMissingAcquiresTypes(
                element,
                elementAcquiredTys,
                outerAcquiredTypes,
                outerFunction,
                outerModule
            )
        }
    }

    private fun checkMissingAcquiresTypes(
        element: MvAcquireTypesOwner,
        elementAcquiredTys: List<Ty>,
        outerAcquiredTys: List<Ty>,
        outerFunction: MvFunction,
        outerModule: MvModule,
    ) {
        val outerAcquiredTypeNames = outerAcquiredTys.map { it.fullnameNoArgs() }.toSet()
        val missingTypeItems = buildList {
            for (elementAcqTy in elementAcquiredTys) {
                // type parameters can be arguments, but only for inline functions
//                    elementAcqTy is TyTypeParameter && outerFunction.isInline -> {
//                        elementAcqTy.origin.takeIf { tyOrigin -> outerAcquiredTys.all { tyOrigin != it } }
//                    }
                if (elementAcqTy is TyAdt) {
                    val acqTyModule = elementAcqTy.item.containingModule

                    // no acquireable in this module, not relevant
                    if (acqTyModule != outerModule) continue

                    if (elementAcqTy.fullnameNoArgs() !in outerAcquiredTypeNames) {
                        add(elementAcqTy.item)
                    }
                }
            }
        }

        if (missingTypeItems.isNotEmpty()) {
            val name = outerFunction.name ?: return
            val missingTypeNames = missingTypeItems.mapNotNull { it.name }
            holder.registerProblem(
                element,
                "Function '$name' is not marked as 'acquires ${missingTypeNames.joinToString()}'",
                ProblemHighlightType.GENERIC_ERROR,
                AddAcquiresFix(outerFunction, missingTypeNames)
            )
        }
    }
}

class MvMissingAcquiresInspection: MvLocalInspectionTool() {

    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        MvMissingAcquiresVisitor(holder)
}
