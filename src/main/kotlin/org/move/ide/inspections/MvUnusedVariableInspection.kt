package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.*

class MvUnusedVariableInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitLetStmt(o: MvLetStmt) {
                val bindings = o.pat?.descendantsOfType<MvBindingPat>().orEmpty()
                for (binding in bindings) {
                    checkUnused(binding, "Unused variable")
                }
            }

            override fun visitFunctionParameter(o: MvFunctionParameter) {
                val binding = o.bindingPat
                checkUnused(binding, "Unused function parameter")
            }

            private fun checkUnused(binding: MvBindingPat, description: String) {
                val bindingName = binding.name ?: return
                if (bindingName.startsWith("_")) return

                val usages = binding.findUsages()
                if (usages.none()) {
                    holder.registerProblem(
                        binding,
                        description,
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        object : LocalQuickFix {
                            override fun getFamilyName(): String {
                                return "Rename to _$bindingName"
                            }
                            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                val newBindingPat = project.psiFactory.bindingPat("_$bindingName")
                                descriptor.psiElement.replace(newBindingPat)
                            }
                        })
                }
            }
        }
}
