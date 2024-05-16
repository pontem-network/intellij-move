/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.cli.runConfigurations.aptos

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.move.cli.runConfigurations.hasMoveProject

abstract class RunAptosCommandActionBase(text: String? = null) : DumbAwareAction(text) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        val hasMoveProject = e.project?.hasMoveProject == true
        e.presentation.isEnabledAndVisible = hasMoveProject
    }
}
