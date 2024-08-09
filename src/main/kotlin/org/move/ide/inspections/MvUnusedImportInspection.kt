package org.move.ide.inspections

import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import org.move.ide.inspections.imports.ImportAnalyzer2

class MvUnusedImportInspection: MvLocalInspectionTool() {

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = ImportAnalyzer2(holder)

    @Suppress("CompanionObjectInExtension")
    companion object {
        fun isEnabled(project: Project): Boolean {
            val profile = InspectionProjectProfileManager.getInstance(project).currentProfile
            return profile.isToolEnabled(HighlightDisplayKey.find(SHORT_NAME))
        }

        const val SHORT_NAME: String = "MvUnusedImport"
    }
}
