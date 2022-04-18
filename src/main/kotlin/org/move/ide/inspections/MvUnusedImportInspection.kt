package org.move.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.move.lang.core.psi.MvVisitor

class MvUnusedImportInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        TODO("Not yet implemented")
    }

}
