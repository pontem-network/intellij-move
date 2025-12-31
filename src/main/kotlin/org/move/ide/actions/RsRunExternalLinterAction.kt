/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.actions

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.actions.RunInspectionIntention
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import org.move.cli.runConfigurations.endless.RunEndlessCommandActionBase
import org.move.ide.inspections.MvExternalLinterInspection

class MvRunExternalLinterAction: RunEndlessCommandActionBase("Run External Linter") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val currentProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
        val wrapper = currentProfile.getInspectionTool(MvExternalLinterInspection.SHORT_NAME, project) ?: return
        val managerEx = InspectionManager.getInstance(project) as InspectionManagerEx

        val analysisScope = AnalysisScope(project)
        RunInspectionIntention.rerunInspection(wrapper, managerEx, analysisScope, null)
    }
}
