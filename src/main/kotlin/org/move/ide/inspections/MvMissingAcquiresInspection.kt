package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.move.ide.presentation.canBeAcquiredInModule
import org.move.ide.presentation.fullnameNoArgs
import org.move.ide.presentation.nameNoArgs
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.getAcquiresTys
import org.move.lang.core.psi.ext.inferAcquiresTys
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.itemContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

class MvMissingAcquiresInspection : MvLocalInspectionTool() {

    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitCallExpr(callExpr: MvCallExpr) {
                val function = callExpr.containingFunction ?: return
                val module = callExpr.containingModule ?: return

                val msl = callExpr.isMsl()
                val itemContext = module.itemContext(msl)
                val declaredTyFullnames = function.getAcquiresTys(itemContext).map { it.fullnameNoArgs() }

                val acquiresTys = callExpr.inferAcquiresTys() ?: return
                val missingTys = acquiresTys
                    .filter { it.fullnameNoArgs() !in declaredTyFullnames }
                    .filter { it !is TyUnknown }
                    .filter { it.canBeAcquiredInModule(module) }

                if (missingTys.isNotEmpty()) {
                    val name = function.name ?: return
                    val missingTyNames = missingTys.joinToString(transform = Ty::nameNoArgs)
                    holder.registerProblem(
                        callExpr,
                        "Function '$name' is not marked as 'acquires $missingTyNames'",
                        ProblemHighlightType.GENERIC_ERROR,
                        object : LocalQuickFix {
                            override fun getFamilyName() = "Add missing acquires"
                            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                val acquiresType = function.acquiresType
                                val psiFactory = project.psiFactory
                                if (acquiresType != null) {
                                    val acquires =
                                        psiFactory.acquires(acquiresType.text + ", " + missingTyNames)
                                    acquiresType.replace(acquires)
                                } else {
                                    val acquires =
                                        psiFactory.acquires("acquires $missingTyNames")
                                    function.addBefore(acquires, function.codeBlock)
                                }
                            }
                        }
                    )
                }
            }
        }
}
