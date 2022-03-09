package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.move.ide.presentation.fullnameNoArgs
import org.move.ide.presentation.declaringModule
import org.move.ide.presentation.nameNoArgs
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.infer.inferCallExprTy
import org.move.lang.core.types.infer.inferenceCtx
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction

class MvMissingAcquiresInspection : MvLocalInspectionTool() {

    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitCallExpr(callExpr: MvCallExpr) {
                val function = callExpr.containingFunction ?: return
                val module = callExpr.containingModule ?: return
                val declaredTyFullnames = function.acquiresTys.map { it.fullnameNoArgs() }

                val ctx = function.inferenceCtx(callExpr.isMsl())
                val callTy = inferCallExprTy(callExpr, ctx) as? TyFunction ?: return
                val missingTys = callTy.acquiresTypes
                    .filter { it.fullnameNoArgs() !in declaredTyFullnames }
                    .filter { it.declaringModule == module }
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
                                        psiFactory.createAcquires(acquiresType.text + ", " + missingTyNames)
                                    acquiresType.replace(acquires)
                                } else {
                                    val acquires =
                                        psiFactory.createAcquires("acquires $missingTyNames")
                                    function.addBefore(acquires, function.codeBlock)
                                }
                            }
                        }
                    )
                }
            }
        }
}
