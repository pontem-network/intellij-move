package org.move.ide.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.move.ide.presentation.canBeAcquiredInModule
import org.move.ide.presentation.fullnameNoArgs
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.inferAcquiresTys
import org.move.lang.core.types.infer.itemContext


class MvUnusedAcquiresTypeInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        fun registerUnusedAcquires(ref: PsiElement) {
            holder.registerProblem(
                ref,
                "Unused acquires clause",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                object : InspectionQuickFix("Remove acquires") {
                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        val element = descriptor.psiElement
                        when (element) {
                            is MvAcquiresType -> element.delete()
                            is MvPathType -> {
                                val acquiresType = element.parent as MvAcquiresType
                                val typeNames =
                                    acquiresType.pathTypeList
                                        .filter { it != element }
                                        .joinToString(", ") { it.text }
                                val newAcquiresType = project.psiFactory.acquires("acquires $typeNames")
                                acquiresType.replace(newAcquiresType)
                            }
                        }
                    }
                }
            )
        }
        return object : MvVisitor() {
            override fun visitAcquiresType(o: MvAcquiresType) {
                val function = o.parent as? MvFunction ?: return
                val module = function.module ?: return
                val codeBlock = function.codeBlock ?: return

                val itemContext = module.itemContext(false)

                val acquiredTys = mutableSetOf<String>()
                for (callExpr in codeBlock.descendantsOfType<MvCallExpr>()) {
                    val callAcquiresTys =
                        callExpr.inferAcquiresTys() ?: return
                    val acqTyNames = callAcquiresTys.map { it.fullnameNoArgs() }
                    acquiredTys.addAll(acqTyNames)
                }

                val unusedAcquiresIndices = mutableListOf<Int>()
                val visitedTypeNames = mutableSetOf<String>()
                val pathTypes = o.pathTypeList
                for ((i, pathType) in pathTypes.withIndex()) {
                    // check that this acquires is allowed in the context
                    val ty = itemContext.getTypeTy(pathType)
                    if (!ty.canBeAcquiredInModule(module)) {
                        unusedAcquiresIndices.add(i)
                        continue
                    }
                    // check for duplicates
                    val typeName = ty.fullnameNoArgs()
                    if (typeName in visitedTypeNames) {
                        unusedAcquiresIndices.add(i)
                        continue
                    }
                    visitedTypeNames.add(typeName)
                    // check for unused
                    if (typeName !in acquiredTys) {
                        unusedAcquiresIndices.add(i)
                        continue
                    }
                }

                if (unusedAcquiresIndices.size == pathTypes.size) {
                    // register whole acquiresType
                    registerUnusedAcquires(o)
                    return
                }
                for (idx in unusedAcquiresIndices) {
                    registerUnusedAcquires(pathTypes[idx])
                }
            }
        }
    }
}
