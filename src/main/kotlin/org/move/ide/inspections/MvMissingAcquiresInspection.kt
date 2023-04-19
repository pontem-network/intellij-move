package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.fixes.AddAcquiresFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isInline
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyTypeParameter

class MvMissingAcquiresInspection : MvLocalInspectionTool() {

    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitCallExpr(callExpr: MvCallExpr) {
                val outerFunction = callExpr.containingFunction ?: return
                if (outerFunction.isInline) return

                val currentModule = outerFunction.module ?: return
                val msl = callExpr.isMsl()
//                val itemContext = currentModule.itemContext(msl)
//                val declaredTyFullnames = itemContext
//                    .getFunctionItemTy(function)
//                    .acquiresTypes
//                    .map { it.fullnameNoArgs() }
                val declaredItems = outerFunction.acquiresType
                    ?.pathTypeList.orEmpty()
                    .mapNotNull { it.path.reference?.resolveWithAliases() }

                val inference = outerFunction.inference(msl)
                val itemTyVars = outerFunction.tyInfers
                val missingItems = inference.getAcquiredTypes(callExpr, outerSubst = itemTyVars)
//                    .map { it.substituteOrUnknown(typeParameters) }
                    .mapNotNull { ty ->
                        when (ty) {
                            is TyTypeParameter -> if (!declaredItems.any { it == ty.origin }) ty.origin else null
                            is TyStruct -> {
                                val notAcquired = ty.item.containingModule == currentModule
                                        && !declaredItems.any { it == ty.item }
                                if (notAcquired) ty.item else null
                            }
                            else -> null
                        }
                    }
                if (missingItems.isNotEmpty()) {
                    val name = outerFunction.name ?: return
                    val missingNames = missingItems.mapNotNull { it.name }
                    holder.registerProblem(
                        callExpr,
                        "Function '$name' is not marked as 'acquires ${missingNames.joinToString()}'",
                        ProblemHighlightType.GENERIC_ERROR,
                        AddAcquiresFix(outerFunction, missingNames)
//                        object : LocalQuickFix {
//                            override fun getFamilyName() = "Add missing acquires"
//                            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
//                                val acquiresType = function.acquiresType
//                                val psiFactory = project.psiFactory
//                                if (acquiresType != null) {
//                                    val acquires =
//                                        psiFactory.acquires(acquiresType.text + ", " + missingNames)
//                                    acquiresType.replace(acquires)
//                                } else {
//                                    val acquires =
//                                        psiFactory.acquires("acquires $missingNames")
//                                    function.addBefore(acquires, function.codeBlock)
//                                }
//                            }
//                        }
                    )
                }
            }
        }
}
