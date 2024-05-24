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
import com.intellij.openapi.util.Key
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.RunAptosCommandActionBase
import org.move.cli.runConfigurations.getAppropriateMoveProject
import org.move.ide.inspections.MvExternalLinterInspection

class MvRunExternalLinterAction: RunAptosCommandActionBase("Run External Linter") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val currentProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
        val wrapper = currentProfile.getInspectionTool(MvExternalLinterInspection.SHORT_NAME, project) ?: return
        val managerEx = InspectionManager.getInstance(project) as InspectionManagerEx
        val inspectionContext = RunInspectionIntention.createContext(wrapper, managerEx, null)

        val cargoProject = getAppropriateMoveProject(e.dataContext)
        inspectionContext.putUserData(CARGO_PROJECT, cargoProject)

        inspectionContext.doInspections(AnalysisScope(project))
    }

    companion object {
        @JvmField
        val CARGO_PROJECT: Key<MoveProject> = Key.create("Move project")
    }
}
