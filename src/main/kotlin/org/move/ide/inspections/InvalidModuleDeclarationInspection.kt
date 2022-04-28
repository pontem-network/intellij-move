package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.address

class InvalidModuleDeclarationInspection : MvLocalInspectionTool() {
    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object : MvVisitor() {
            override fun visitModule(mod: MvModule) {
                val identifier = mod.identifier ?: return
                if (mod.address() == null) {
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
