package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.move.ide.inspections.fixes.RemoveAcquiresFix
import org.move.ide.presentation.fullnameNoArgs
import org.move.ide.presentation.itemDeclaredInModule
import org.move.lang.core.psi.*
import org.move.lang.core.types.infer.acquiresContext
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.moveProject


class MvUnusedAcquiresTypeInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        val annotationHolder = Holder(holder)
        return object : MvVisitor() {
            override fun visitAcquiresType(o: MvAcquiresType) {
                val function = o.parent as? MvFunction ?: return
                val currentModule = function.module ?: return
                val acquiresContext = o.moveProject?.acquiresContext ?: return
                val inference = function.inference(false)

                val callAcquiresTypes = mutableSetOf<String>()
                for (callExpr in inference.callableTypes.keys) {
                    val types = acquiresContext.getCallTypes(callExpr, inference)
                    callAcquiresTypes.addAll(
                        types.map { it.fullnameNoArgs() })
                }

                val unusedTypeIndices = mutableListOf<Int>()
                val visitedTypes = mutableSetOf<String>()
                for ((i, pathType) in function.acquiresPathTypes.withIndex()) {
                    val ty = pathType.loweredType(false)
                    if (!ty.itemDeclaredInModule(currentModule)) {
                        unusedTypeIndices.add(i)
                        continue
                    }

                    // check for duplicates
                    val tyFullName = ty.fullnameNoArgs()
                    if (tyFullName in visitedTypes) {
                        unusedTypeIndices.add(i)
                        continue
                    }
                    visitedTypes.add(tyFullName)

                    if (tyFullName !in callAcquiresTypes) {
                        unusedTypeIndices.add(i)
                        continue
                    }
                }
                if (unusedTypeIndices.size == function.acquiresPathTypes.size) {
                    // register whole acquiresType
                    annotationHolder.registerUnusedAcquires(o)
                    return
                }
                for (idx in unusedTypeIndices) {
                    annotationHolder.registerUnusedAcquires(function.acquiresPathTypes[idx])
                }
            }
        }
    }

    class Holder(val problemsHolder: ProblemsHolder) {
        fun registerUnusedAcquires(ref: PsiElement) {
            problemsHolder.registerProblem(
                ref,
                "Unused acquires clause",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RemoveAcquiresFix(ref)
            )
        }
    }
}
