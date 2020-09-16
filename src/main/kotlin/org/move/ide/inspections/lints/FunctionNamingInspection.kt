package org.move.ide.inspections.lints

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveNativeFunctionDef
import org.move.lang.core.psi.MoveVisitor

class FunctionNamingInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Attempt to override a built-in function"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : MoveVisitor() {
            override fun visitFunctionDef(o: MoveFunctionDef) {
                warnOnBuiltInFunctionName(o, holder)
            }

            override fun visitNativeFunctionDef(o: MoveNativeFunctionDef) {
                warnOnBuiltInFunctionName(o, holder)
            }
        }

    private fun warnOnBuiltInFunctionName(element: MoveNamedElement, holder: ProblemsHolder) {
        val nameElement = element.nameElement ?: return
        val name = element.name ?: return
        if (name in BUILTIN_FUNCTIONS) {
            holder.registerProblem(nameElement,
                "Invalid function name: `$name` is a built-in function")
        }
    }
}