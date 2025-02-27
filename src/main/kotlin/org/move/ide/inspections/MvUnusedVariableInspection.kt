package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.descendantsOfType
import org.move.ide.inspections.fixes.RemoveParameterFix
import org.move.ide.inspections.fixes.RenameFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.bindingTypeOwner
import org.move.lang.core.psi.ext.isMsl

class MvUnusedVariableInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object: MvVisitor() {
            override fun visitLetStmt(o: MvLetStmt) {
                val bindings = o.pat?.descendantsOfType<MvPatBinding>().orEmpty()
                for (binding in bindings) {
                    checkUnused(binding, "Unused variable")
                }
            }

            override fun visitFunctionParameter(o: MvFunctionParameter) {
                val functionLike = o.containingFunctionLike ?: return
                if (functionLike.anyBlock == null) return

                val binding = o.patBinding
                checkUnused(binding, "Unused function parameter")
            }

            private fun checkUnused(binding: MvPatBinding, description: String) {
                if (binding.isMsl()) return

                val bindingName = binding.name
                if (bindingName.startsWith("_")) return

                val searchHelper = PsiSearchHelper.getInstance(binding.project)
                val usageScope = searchHelper.getCodeUsageScope(binding)
                val references =
                    ReferencesSearch.search(binding, usageScope)
                        // filter out `#[test(signer1, signer2)]` declarations
                        .filter { it.element.parent !is MvAttrItem }
                if (references.none()) {
                    val fixes = when (binding.bindingTypeOwner) {
                        is MvFunctionParameter -> arrayOf(
                            RenameFix(binding, "_$bindingName"),
                            RemoveParameterFix(binding, bindingName)
                        )
                        else -> arrayOf(RenameFix(binding, "_$bindingName"))
                    }
                    holder.registerProblem(
                        binding,
                        description,
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        *fixes
                    )
                }
            }
        }
}
