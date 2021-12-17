package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.definedAddressRef

class InvalidModuleDeclarationInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object : MvVisitor() {
            override fun visitModuleDef(mod: MvModuleDef) {
                val identifier = mod.identifier ?: return
                if (mod.definedAddressRef() == null) {
                    holder.registerProblem(
                        identifier,
                        "Invalid module declaration. The module does not have a specified address / address block.",
                        ProblemHighlightType.ERROR
                    )
                }
            }
        }
    }
}
