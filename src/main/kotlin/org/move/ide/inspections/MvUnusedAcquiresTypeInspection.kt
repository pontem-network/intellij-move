package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.move.ide.inspections.fixes.RemoveAcquiresFix
import org.move.ide.presentation.canBeAcquiredInModule
import org.move.ide.presentation.fullnameNoArgs
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.inferAcquiresTys
import org.move.lang.core.types.infer.inferenceContext


class MvUnusedAcquiresTypeInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        fun registerUnusedAcquires(ref: PsiElement) {
            holder.registerProblem(
                ref,
                "Unused acquires clause",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RemoveAcquiresFix(ref)
            )
        }
        return object : MvVisitor() {
            override fun visitAcquiresType(o: MvAcquiresType) {
                val function = o.parent as? MvFunction ?: return
                val module = function.module ?: return
                val codeBlock = function.codeBlock ?: return

                val inferenceCtx = function.inferenceContext(false)

                val acquiredTys = mutableSetOf<String>()
                for (callExpr in codeBlock.descendantsOfType<MvCallExpr>()) {
                    val callAcquiresTys = callExpr.inferAcquiresTys() ?: return
                    val acqTyNames = callAcquiresTys.map { it.fullnameNoArgs() }
                    acquiredTys.addAll(acqTyNames)
                }

                val unusedAcquiresIndices = mutableListOf<Int>()
                val visitedTypeNames = mutableSetOf<String>()
                val pathTypes = o.pathTypeList
                for ((i, pathType) in pathTypes.withIndex()) {
                    // check that this acquires is allowed in the context
                    val ty = inferenceCtx.getTypeTy(pathType)
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
